package com.logatron.agent.web;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class ShipperDtos {
    private ShipperDtos(){}
    public record GenerateRequest(@NotNull UUID agentId){}
    public record DeployRequest(@Positive long version,Long expectedSelectedVersion,boolean confirmed){}
    public record DeploymentSettings(boolean enabled,boolean confirmed){}
    public record DestinationView(String id,String name,String endpoint,boolean insecure){}
    public record RevisionView(UUID id,long version,String sha256,int sourceCount,Instant createdAt,String validationStatus,String validationMessage,String yaml,List<String> warnings){}
    public record DeploymentView(UUID id,long version,Long previousVersion,String action,String status,Instant requestedAt,Instant completedAt,String error){}
    public record ShipperView(UUID agentId,UUID serverId,Long selectedVersion,boolean deploymentEnabled,boolean deploymentAvailable,String deploymentReason,String protocol,String lastIngestionAt,List<RevisionView> revisions,List<DeploymentView> deployments){}
}
