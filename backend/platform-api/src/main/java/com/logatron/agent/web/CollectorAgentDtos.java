package com.logatron.agent.web;

import com.logatron.catalog.domain.CatalogEnums.CollectorStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class CollectorAgentDtos {
    private CollectorAgentDtos() {}

    public record RegisterAgentRequest(
            @NotNull UUID serverId,
            @NotBlank @Pattern(regexp="[A-Za-z0-9._-]{3,160}") String agentKey,
            @NotBlank @Size(max=255) String gatewayEndpoint,
            boolean gatewayInsecure) {}

    public record UpdateAgentGatewayRequest(
            @NotBlank @Size(max=255) String gatewayEndpoint,
            boolean gatewayInsecure) {}

    public record AgentRegistration(AgentView agent,String token) {}

    public record AgentView(
            UUID id,
            UUID serverId,
            String agentKey,
            String gatewayEndpoint,
            boolean gatewayInsecure,
            CollectorStatus status,
            Instant lastSeenAt,
            String collectorVersion,
            long desiredConfigVersion,
            String desiredConfigHash,
            Long appliedConfigVersion,
            String appliedConfigHash,
            boolean inSync,
            int sourceCount,
            String lastError) {}

    public record AgentHeartbeatRequest(
            @Size(max=80) String collectorVersion,
            @Size(max=64) String appliedConfigHash,
            @PositiveOrZero Long appliedConfigVersion,
            CollectorStatus status,
            @Size(max=4000) String lastError) {}

    public record ConfigRevisionView(long configVersion,String configHash,int sourceCount,Instant createdAt,boolean applied) {}
}
