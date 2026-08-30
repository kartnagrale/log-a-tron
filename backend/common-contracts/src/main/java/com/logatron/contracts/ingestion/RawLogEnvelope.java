package com.logatron.contracts.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawLogEnvelope(int schemaVersion, UUID eventId, Instant observedTimestamp,
        Collector collector, Resource resource, Source source, String payload, Map<String,Object> attributes) {
    public RawLogEnvelope {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Collector(String collectorId, String sourceOffset) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resource(UUID projectId, UUID environmentId, UUID serverId, UUID serviceId,
            UUID serviceInstanceId) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(UUID logSourceId, String logFile, String logType, ParserProfile parserProfile) {}
}