package com.logatron.catalog.web;

import com.logatron.catalog.domain.CatalogEnums.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class CatalogDtos {
    private CatalogDtos() {}

    public record ProjectView(UUID id,String slug,String name,Classification classification,Status status,long environments,long servers,long services,long instances) {}
    public record EnvironmentView(UUID id,UUID projectId,String code,String name,EnvironmentType type,Sensitivity sensitivity,Status status) {}
    public record ServerView(UUID id,UUID environmentId,String hostId,String hostname,String ipAddress,Status status,Map<String,String> labels,CollectorStatus collectorStatus,Instant collectorLastSeenAt) {}
    public record ServiceView(UUID id,UUID projectId,String serviceKey,String displayName,String owner,Criticality criticality,Status status,Map<String,String> labels) {}
    public record ServiceInstanceView(UUID id,UUID serviceId,String serviceName,UUID serverId,String hostname,String instanceKey,String version,Status status,Instant firstSeenAt,Instant lastSeenAt) {}

    public record LogSourceView(
            UUID id,UUID serviceInstanceId,String pathPattern,String logType,String parserProfile,
            String multilineRule,String timestampTimezone,boolean enabled,long configVersion) {
        public LogSourceView(UUID id,UUID serviceInstanceId,String pathPattern,String logType,String parserProfile,String multilineRule,boolean enabled,long configVersion) {
            this(id,serviceInstanceId,pathPattern,logType,parserProfile,multilineRule,"UTC",enabled,configVersion);
        }
    }

    public record CreateProjectRequest(@NotNull UUID companyId,@NotBlank @Pattern(regexp="[a-z0-9-]{2,80}") String slug,@NotBlank @Size(max=200) String name,@NotNull Classification classification) {}
    public record UpdateProjectRequest(@NotBlank @Size(max=200) String name,@NotNull Classification classification,@NotNull Status status) {}
    public record CreateEnvironmentRequest(@NotNull UUID projectId,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{2,40}") String code,@NotBlank @Size(max=120) String name,@NotNull EnvironmentType type,@NotNull Sensitivity sensitivity) {}
    public record UpdateEnvironmentRequest(@NotBlank @Size(max=120) String name,@NotNull EnvironmentType type,@NotNull Sensitivity sensitivity,@NotNull Status status) {}
    public record CreateServerRequest(@NotNull UUID environmentId,@NotBlank @Size(max=160) String hostId,@NotBlank @Size(max=255) String hostname,@Size(max=45) String ipAddress,Map<String,String> labels) {}
    public record UpdateServerRequest(@NotBlank @Size(max=255) String hostname,@Size(max=45) String ipAddress,@NotNull Status status,Map<String,String> labels) {}
    public record CreateServiceRequest(@NotNull UUID projectId,@NotBlank @Pattern(regexp="[a-z0-9-]{2,120}") String serviceKey,@NotBlank @Size(max=200) String displayName,@Size(max=200) String owner,@NotNull Criticality criticality,Map<String,String> labels) {}
    public record UpdateServiceRequest(@NotBlank @Size(max=200) String displayName,@Size(max=200) String owner,@NotNull Criticality criticality,@NotNull Status status,Map<String,String> labels) {}
    public record CreateInstanceRequest(@NotNull UUID serviceId,@NotNull UUID serverId,@NotBlank @Size(max=180) String instanceKey,@Size(max=80) String version) {}
    public record UpdateInstanceRequest(@Size(max=80) String version,@NotNull Status status) {}

    public record CreateLogSourceRequest(
            @NotNull UUID serviceInstanceId,
            @NotBlank @Size(max=1000) String pathPattern,
            @NotBlank @Size(max=60) String logType,
            @NotBlank @Size(max=100) String parserProfile,
            @Size(max=4000) String multilineRule,
            @Size(max=80) String timestampTimezone,
            boolean enabled) {
        public CreateLogSourceRequest(UUID serviceInstanceId,String pathPattern,String logType,String parserProfile,String multilineRule,boolean enabled) {
            this(serviceInstanceId,pathPattern,logType,parserProfile,multilineRule,"UTC",enabled);
        }
    }

    public record UpdateLogSourceRequest(
            @NotBlank @Size(max=60) String logType,
            @NotBlank @Size(max=100) String parserProfile,
            @Size(max=4000) String multilineRule,
            @Size(max=80) String timestampTimezone,
            boolean enabled) {
        public UpdateLogSourceRequest(String logType,String parserProfile,String multilineRule,boolean enabled) {
            this(logType,parserProfile,multilineRule,"UTC",enabled);
        }
    }
}
