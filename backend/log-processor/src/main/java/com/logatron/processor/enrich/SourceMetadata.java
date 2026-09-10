package com.logatron.processor.enrich;

import com.logatron.contracts.ingestion.ParserProfile;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

public record SourceMetadata(
        UUID companyId,
        String company,
        UUID projectId,
        String project,
        String classification,
        UUID environmentId,
        String environment,
        UUID serverId,
        String server,
        String hostname,
        String serverIp,
        UUID serviceId,
        String service,
        UUID serviceInstanceId,
        String serviceInstance,
        UUID logSourceId,
        String logFile,
        String logType,
        ParserProfile parserProfile,
        ZoneId timestampZone) {

    public SourceMetadata(
            UUID companyId,String company,UUID projectId,String project,String classification,
            UUID environmentId,String environment,UUID serverId,String server,String hostname,String serverIp,
            UUID serviceId,String service,UUID serviceInstanceId,String serviceInstance,UUID logSourceId,
            String logFile,String logType,ParserProfile parserProfile) {
        this(companyId,company,projectId,project,classification,environmentId,environment,serverId,server,hostname,serverIp,
                serviceId,service,serviceInstanceId,serviceInstance,logSourceId,logFile,logType,parserProfile,ZoneOffset.UTC);
    }
}
