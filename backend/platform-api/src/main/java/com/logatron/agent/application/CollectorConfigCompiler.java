package com.logatron.agent.application;

import com.logatron.agent.configuration.AgentManagementProperties;
import com.logatron.agent.persistence.CollectorAgentEntity;
import com.logatron.catalog.persistence.LogSourceEntity;
import com.logatron.catalog.persistence.LogSourceRepository;
import com.logatron.contracts.ingestion.ParserProfile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Component
public class CollectorConfigCompiler {
    private final LogSourceRepository sources;
    private final AgentManagementProperties properties;

    public CollectorConfigCompiler(LogSourceRepository sources,AgentManagementProperties properties){this.sources=sources;this.properties=properties;}

    public CompiledCollectorConfig compile(CollectorAgentEntity agent){
        List<LogSourceEntity> rows=sources.findEnabledDetailedByServerId(agent.getServer().getId());
        var server=agent.getServer();var environment=server.getEnvironment();var project=environment.getProject();
        StringBuilder yaml=new StringBuilder();
        yaml.append("extensions:\n")
                .append("  file_storage:\n")
                .append("    directory: ").append(q(properties.stateDirectory()+"/storage")).append("\n")
                .append("    create_directory: true\n\n")
                .append("receivers:\n")
                .append("  otlp/app:\n")
                .append("    protocols:\n")
                .append("      grpc:\n        endpoint: 127.0.0.1:4317\n")
                .append("      http:\n        endpoint: 127.0.0.1:4318\n");
        for(LogSourceEntity source:rows)appendReceiver(yaml,source);

        yaml.append("\nprocessors:\n")
                .append("  resource/agent_context:\n")
                .append("    attributes:\n")
                .append(resource("logatron.project.id",project.getId().toString()))
                .append(resource("logatron.environment.id",environment.getId().toString()))
                .append(resource("logatron.server.id",server.getId().toString()))
                .append(resource("deployment.environment.name",environment.getCode()))
                .append("  batch/signals:\n    timeout: 1s\n    send_batch_size: 100\n");
        for(LogSourceEntity source:rows){appendTransform(yaml,agent,source);appendBatch(yaml,source);}

        yaml.append("\nexporters:\n")
                .append("  otlp/logatron:\n")
                .append("    endpoint: ").append(q(agent.getGatewayEndpoint())).append("\n")
                .append("    tls:\n      insecure: ").append(agent.isGatewayInsecure()).append("\n")
                .append("    sending_queue:\n      enabled: true\n      storage: file_storage\n      queue_size: 10000\n")
                .append("    retry_on_failure:\n      enabled: true\n      initial_interval: 1s\n      max_interval: 10s\n      max_elapsed_time: 0s\n\n")
                .append("service:\n  extensions: [file_storage]\n  pipelines:\n")
                .append("    traces/app:\n      receivers: [otlp/app]\n      processors: [resource/agent_context, batch/signals]\n      exporters: [otlp/logatron]\n")
                .append("    metrics/app:\n      receivers: [otlp/app]\n      processors: [resource/agent_context, batch/signals]\n      exporters: [otlp/logatron]\n");
        for(LogSourceEntity source:rows){String key=key(source);yaml.append("    logs/").append(key).append(":\n")
                .append("      receivers: [filelog/").append(key).append("]\n")
                .append("      processors: [transform/").append(key).append(", batch/").append(key).append("]\n")
                .append("      exporters: [otlp/logatron]\n");}
        String text=yaml.toString();return new CompiledCollectorConfig(text,sha256(text),rows.size());
    }

    private void appendReceiver(StringBuilder y,LogSourceEntity source){
        String key=key(source);y.append("  filelog/").append(key).append(":\n")
                .append("    include:\n      - ").append(q(source.getPathPattern())).append("\n")
                .append("    start_at: end\n")
                .append("    include_file_path: true\n")
                .append("    include_file_name: true\n")
                .append("    max_log_size: 200KiB\n")
                .append("    storage: file_storage\n");
        String multiline=multiline(source);if(multiline!=null)y.append("    multiline:\n      line_start_pattern: ").append(q(multiline)).append("\n");
    }

    private void appendTransform(StringBuilder y,CollectorAgentEntity agent,LogSourceEntity source){
        var instance=source.getServiceInstance();var server=instance.getServer();var environment=server.getEnvironment();var service=instance.getService();var project=service.getProject();
        String profile=ParserProfile.fromExternalName(source.getParserProfile()).name();String key=key(source);
        String template="{\"schemaVersion\":1,\"eventId\":\"\",\"observedTimestamp\":\"\",\"collector\":{\"collectorId\":\"\",\"sourceOffset\":\"\"},\"resource\":{\"projectId\":\"\",\"environmentId\":\"\",\"serverId\":\"\",\"serviceId\":\"\",\"serviceInstanceId\":\"\"},\"source\":{\"logSourceId\":\"\",\"logFile\":\"\",\"logType\":\"application\",\"parserProfile\":\""+profile+"\"},\"payload\":\"\",\"attributes\":{}}";
        y.append("  transform/").append(key).append(":\n    error_mode: ignore\n    log_statements:\n      - context: log\n        statements:\n")
                .append(stmt("set(attributes[\"__logatron_payload\"], body)"))
                .append(stmt("replace_pattern(attributes[\"__logatron_payload\"], \"(?i)(authorization\\\\s*[:=]\\\\s*bearer\\\\s+)[^\\\\s,;\\\\\"]+\", \"$1[REDACTED]\")"))
                .append(stmt("replace_pattern(attributes[\"__logatron_payload\"], \"(?i)((?:password|passwd|api[-]?key|access[-]?token|refresh[-]?token|secret)\\\\s*[:=]\\\\s*[\\\\\"]?)[^\\\\s,;&\\\\\"]+\", \"$1[REDACTED]\")"))
                .append(stmt("replace_pattern(attributes[\"__logatron_payload\"], \"(?i)(jdbc:[a-z0-9]+://[^:/@\\\\s]+:)[^@/\\\\s]+(@)\", \"$1[REDACTED]$2\")"))
                .append(stmt("merge_maps(cache, ParseJSON(\""+escapeOttl(template)+"\"), \"upsert\")"))
                .append(stmt("set(cache[\"eventId\"], UUID())"))
                .append(stmt("set(cache[\"observedTimestamp\"], FormatTime(Now(), \"2006-01-02T15:04:05.000-07:00\"))"))
                .append(stmt("set(cache[\"collector\"][\"collectorId\"], \""+escapeOttl(agent.getAgentKey())+"\")"))
                .append(stmt("set(cache[\"collector\"][\"sourceOffset\"], attributes[\"log.file.path\"])"))
                .append(stmt("set(cache[\"resource\"][\"projectId\"], \""+project.getId()+"\")"))
                .append(stmt("set(cache[\"resource\"][\"environmentId\"], \""+environment.getId()+"\")"))
                .append(stmt("set(cache[\"resource\"][\"serverId\"], \""+server.getId()+"\")"))
                .append(stmt("set(cache[\"resource\"][\"serviceId\"], \""+service.getId()+"\")"))
                .append(stmt("set(cache[\"resource\"][\"serviceInstanceId\"], \""+instance.getId()+"\")"))
                .append(stmt("set(cache[\"source\"][\"logSourceId\"], \""+source.getId()+"\")"))
                .append(stmt("set(cache[\"source\"][\"logFile\"], attributes[\"log.file.path\"])"))
                .append(stmt("set(cache[\"source\"][\"logType\"], \""+escapeOttl(source.getLogType())+"\")"))
                .append(stmt("set(cache[\"payload\"], attributes[\"__logatron_payload\"])"))
                .append(stmt("set(body, cache)"))
                .append(stmt("delete_key(attributes, \"__logatron_payload\")"));
    }

    private void appendBatch(StringBuilder y,LogSourceEntity source){String key=key(source);y.append("  batch/").append(key).append(":\n    timeout: 1s\n    send_batch_size: 10\n");}
    private static String key(LogSourceEntity source){return "source_"+source.getId().toString().replace("-","").substring(0,12).toLowerCase(Locale.ROOT);}
    private static String multiline(LogSourceEntity source){ParserProfile profile=ParserProfile.fromExternalName(source.getParserProfile());return switch(profile){
        case JAVA_PIPE_V1 -> "^[^\\s|]+\\s*\\|\\s*(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|ERR|FATAL|CRITICAL)\\s*\\|";
        case JAVA_PIPE_LEVEL_FIRST_V1 -> "^\\s*(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|ERR|FATAL|CRITICAL)\\s*\\|";
        case PLAINTEXT -> "^\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}";
        case JSON -> null;
    };}
    private static String resource(String key,String value){return "      - key: "+key+"\n        value: "+q(value)+"\n        action: upsert\n";}
    private static String stmt(String value){return "          - '"+value.replace("'","''")+"'\n";}
    private static String q(String value){return "'"+(value==null?"":value.replace("'","''"))+"'";}
    private static String escapeOttl(String value){return value.replace("\\","\\\\").replace("\"","\\\"");}
    private static String sha256(String value){try{MessageDigest digest=MessageDigest.getInstance("SHA-256");return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("SHA-256 is unavailable",e);}}

    public record CompiledCollectorConfig(String yaml,String sha256,int sourceCount){}
}
