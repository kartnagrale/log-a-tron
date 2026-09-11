package com.logatron.agent.web;

import com.logatron.catalog.domain.CatalogEnums.CollectorStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class CollectorAgentDtos {
    private CollectorAgentDtos() {}

    public record RegisterAgentRequest(
            @NotNull UUID serverId,
            @NotBlank @Pattern(regexp="[A-Za-z0-9._-]{3,160}") String agentKey) {}

    public record AgentRegistration(AgentView agent,String token) {}

    public record AgentView(
            UUID id,
            UUID serverId,
            String agentKey,
            CollectorStatus status,
            Instant lastSeenAt,
            String collectorVersion,
            String desiredConfigHash,
            String appliedConfigHash,
            boolean inSync,
            int sourceCount,
            String lastError) {}

    public record AgentHeartbeatRequest(
            @Size(max=80) String collectorVersion,
            @Size(max=64) String appliedConfigHash,
            CollectorStatus status,
            @Size(max=4000) String lastError) {}
}
