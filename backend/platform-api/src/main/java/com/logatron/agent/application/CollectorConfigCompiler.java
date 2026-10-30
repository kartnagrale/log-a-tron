package com.logatron.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logatron.agent.configuration.*;
import com.logatron.agent.persistence.CollectorAgentEntity;
import com.logatron.catalog.persistence.*;
import com.logatron.catalog.domain.*;
import com.logatron.common.error.ApiException;
import com.logatron.contracts.ingestion.ParserProfile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class CollectorConfigCompiler {
    private final LogSourceRepository sources;
    private final AgentManagementProperties properties;
    private final ShipperManagementProperties settings;
    private final ObjectMapper json=new ObjectMapper();
    @Autowired public CollectorConfigCompiler(LogSourceRepository sources,AgentManagementProperties properties,ShipperManagementProperties settings){this.sources=sources;this.properties=properties;this.settings=settings;}
    public CollectorConfigCompiler(LogSourceRepository sources,AgentManagementProperties properties){this(sources,properties,new ShipperManagementProperties(null,null));}

    public CompiledCollectorConfig compile(CollectorAgentEntity agent){
        List<LogSourceEntity> rows=new ArrayList<>(sources.findEnabledDetailedByServerId(agent.getServer().getId()));
        rows.sort(Comparator.comparing((LogSourceEntity s)->s.getId().toString()));
        var server=agent.getServer();var environment=server.getEnvironment();var project=environment.getProject();
        Map<String,Object> receivers=map("otlp/app",map("protocols",map("grpc",map("endpoint","127.0.0.1:4317"),"http",map("endpoint","127.0.0.1:4318"))));
        Map<String,Object> processors=map("resource/agent_context",map("attributes",List.of(resource("logatron.project.id",project.getId()),resource("logatron.environment.id",environment.getId()),resource("logatron.server.id",server.getId()),resource("deployment.environment.name",environment.getCode()))),"batch/signals",batch(100));
        Map<String,Object> pipelines=map("traces/app",pipeline("otlp/app",List.of("resource/agent_context","batch/signals")),"metrics/app",pipeline("otlp/app",List.of("resource/agent_context","batch/signals")));
        List<String> warnings=new ArrayList<>();
        for(int n=0;n<rows.size();n++){
            var source=rows.get(n);var instance=source.getServiceInstance();
            if(!instance.getServer().getId().equals(server.getId())||!instance.getService().getProject().getId().equals(project.getId()))throw ApiException.invalid("Source ownership is inconsistent with the selected server.");
            var options=source.getShipperOptions();LogSourceRules.validate(source.getPathPattern(),options,source.getMultilineRule());
            String key=key(source);
            Map<String,Object> receiver=map("include",LogSourceRules.includes(source.getPathPattern(),options),"exclude",options.excludes(),"start_at",options.startAt(),"include_file_path",true,"include_file_record_offset",true,"preserve_leading_whitespaces",true,"preserve_trailing_whitespaces",true,"max_log_size",options.maxLogSize(),"storage","file_storage","retry_on_failure",map("enabled",true));
            String multiline=multiline(source);if(multiline!=null)receiver.put("multiline",map("line_start_pattern",multiline));
            var excludes=new LinkedHashSet<String>(options.excludes());
            for(int p=0;p<n;p++){
                var previous=rows.get(p);var left=LogSourceRules.includes(previous.getPathPattern(),previous.getShipperOptions());var right=LogSourceRules.includes(source.getPathPattern(),options);
                boolean overlap=left.stream().anyMatch(a->right.stream().anyMatch(b->LogSourceRules.overlaps(a,b)));
                if(overlap){
                    // Deterministic priority: lower UUID source owns matching files. Exclude it from later receivers.
                    excludes.addAll(left);receiver.put("exclude",List.copyOf(excludes));
                    warnings.add("Overlapping sources "+previous.getId()+" and "+source.getId()+": lower UUID source has priority; its include patterns are excluded from the later receiver.");
                }
            }
            receivers.put("filelog/"+key,receiver);
            processors.put("transform/"+key,map("error_mode","propagate","log_statements",List.of(map("context","log","statements",statements(agent,source)))));
            processors.put("batch/"+key,batch(10));pipelines.put("logs/"+key,pipeline("filelog/"+key,List.of("transform/"+key,"batch/"+key)));
        }
        Map<String,Object> tls=map("insecure",agent.isGatewayInsecure());
        settings.destinations().stream().filter(d->d.endpoint().equals(agent.getGatewayEndpoint())&&d.insecure()==agent.isGatewayInsecure()&&d.permits(project.getId(),environment.getId())).findFirst().ifPresent(d->{
            if(d.caFile()!=null&&!d.caFile().isBlank())tls.put("ca_file",d.caFile());
            if(d.certFile()!=null&&!d.certFile().isBlank())tls.put("cert_file",d.certFile());
            if(d.keyFile()!=null&&!d.keyFile().isBlank())tls.put("key_file",d.keyFile());
        });
        var document=map("extensions",map("health_check",map("endpoint","127.0.0.1:13133"),"file_storage",map("directory",properties.stateDirectory()+"/storage","create_directory",true)),"receivers",receivers,"processors",processors,
            "exporters",map("otlp/logatron",map("endpoint",agent.getGatewayEndpoint(),"tls",tls,"sending_queue",map("enabled",true,"storage","file_storage","queue_size",10000),"retry_on_failure",map("enabled",true,"initial_interval","1s","max_interval","10s","max_elapsed_time","0s"))),
            "service",map("extensions",List.of("health_check","file_storage"),"pipelines",pipelines));
        var dump=new DumperOptions();dump.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);dump.setSplitLines(false);dump.setWidth(4096);
        String yaml=new Yaml(dump).dump(document).replace("$","$");return new CompiledCollectorConfig(yaml,sha256(yaml),rows.size(),List.copyOf(warnings));
    }
    private List<String> statements(CollectorAgentEntity agent,LogSourceEntity source){
        var i=source.getServiceInstance();var n=i.getServer();var e=n.getEnvironment();var s=i.getService();
        String profile=ParserProfile.fromExternalName(source.getParserProfile()).name();
        var envelope=map("schemaVersion",1,"eventId","","observedTimestamp","","collector",map("collectorId",agent.getAgentKey(),"sourceOffset",""),"resource",map("projectId",s.getProject().getId().toString(),"environmentId",e.getId().toString(),"serverId",n.getId().toString(),"serviceId",s.getId().toString(),"serviceInstanceId",i.getId().toString()),"source",map("logSourceId",source.getId().toString(),"logFile","","logType",source.getLogType(),"parserProfile",profile),"payload","","attributes",map());
        try{return List.of(
            "merge_maps(cache, ParseJSON("+json.writeValueAsString(json.writeValueAsString(envelope))+"), \"upsert\")",
            "set(cache[\"eventId\"], UUID())",
            "set(cache[\"observedTimestamp\"], FormatTime(Now(), \"2006-01-02T15:04:05.000000000Z07:00\"))",
            "set(cache[\"collector\"][\"sourceOffset\"], Concat([\""+source.getId()+"\", attributes[\"log.file.path\"], String(attributes[\"log.file.record_offset\"]), SHA256(body)], \"|\"))",
            "set(cache[\"source\"][\"logFile\"], attributes[\"log.file.path\"])",
            "set(cache[\"payload\"], body)","set(body, cache)");
        }catch(Exception ex){throw new IllegalStateException("Unable to serialize envelope",ex);}
    }
    private static String key(LogSourceEntity s){return "source_"+s.getId().toString().replace("-","");}
    private static Map<String,Object> pipeline(String receiver,List<String> processors){return map("receivers",List.of(receiver),"processors",processors,"exporters",List.of("otlp/logatron"));}
    private static Map<String,Object> batch(int size){return map("timeout","1s","send_batch_size",size);}
    private static Map<String,Object> resource(String key,Object value){return map("key",key,"value",value.toString(),"action","upsert");}
    public static Map<String,Object> map(Object...pairs){var map=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)map.put((String)pairs[i],pairs[i+1]);return map;}
    private static String multiline(LogSourceEntity source){
        if(source.getMultilineRule()!=null)return source.getMultilineRule().isBlank()?null:source.getMultilineRule();
        return switch(ParserProfile.fromExternalName(source.getParserProfile())){
            case JAVA_PIPE_V1 -> "^[^\\s|]+\\s*\\|\\s*(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|ERR|FATAL|CRITICAL)\\s*\\|";
            case JAVA_PIPE_LEVEL_FIRST_V1 -> "^\\s*(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|ERR|FATAL|CRITICAL)\\s*\\|";
            case PLAINTEXT -> "^\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}";
            case JSON -> null;
        };
    }
    private static String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    public record CompiledCollectorConfig(String yaml,String sha256,int sourceCount,List<String> warnings){}
}
