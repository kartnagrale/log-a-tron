package com.logatron.contracts.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CanonicalLogEvent(
 int schemaVersion, UUID eventId, Instant timestamp, Instant observedTimestamp, Instant ingestedAt,
 String company, UUID companyId, String project, UUID projectId, String environment, UUID environmentId,
 String server, UUID serverId, String hostname, String serverIp, String service, UUID serviceId,
 String serviceInstance, UUID serviceInstanceId, String logFile, String logType, String level,
 int severityNumber, String traceId, String spanId, String traceFlags, String requestId,
 String correlationId, String transactionId, String orderToken, String auctionId, String logger,
 String thread, String message, String exceptionType, String stackTrace, Map<String,String> attributes,
 Map<String,String> resourceAttributes, String dataClassification, boolean maskingApplied,
 String fingerprint, String collectorId, String sourceOffset) {
 public CanonicalLogEvent {
   attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
   resourceAttributes = resourceAttributes == null ? Map.of() : Map.copyOf(resourceAttributes);
 }
}