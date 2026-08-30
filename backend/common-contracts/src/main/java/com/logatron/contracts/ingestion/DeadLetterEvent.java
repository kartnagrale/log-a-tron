package com.logatron.contracts.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeadLetterEvent(int schemaVersion, UUID eventId, String failureStage, String failureCode,
        String failureReason, Instant timestamp, UUID projectId, UUID logSourceId, int retryCount,
        String redactedPayloadPreview, String payloadHash) {}