package com.logatron.auth.application;
import com.logatron.auth.domain.PlatformRole;import java.util.*;
public record EffectiveScope(boolean globalAdmin,Map<UUID,ProjectScope> projects){public EffectiveScope{projects=Map.copyOf(projects);}public record ProjectScope(UUID projectId,Set<PlatformRole> roles,Set<UUID> environmentIds,Map<UUID,Set<UUID>> serviceIdsByEnvironment,boolean allEnvironments){public ProjectScope{roles=Set.copyOf(roles);environmentIds=Set.copyOf(environmentIds);serviceIdsByEnvironment=serviceIdsByEnvironment.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,e->Set.copyOf(e.getValue())));}}}

