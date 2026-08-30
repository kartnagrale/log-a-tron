package com.logatron.auth.application;
import java.util.UUID;
public record RequestedResourceScope(UUID projectId,UUID environmentId,UUID serviceId){public static RequestedResourceScope project(UUID id){return new RequestedResourceScope(id,null,null);}public static RequestedResourceScope environment(UUID projectId,UUID environmentId){return new RequestedResourceScope(projectId,environmentId,null);}}

