package com.logatron.auth.application;

import com.logatron.auth.domain.PlatformRole;
import com.logatron.auth.persistence.*;
import com.logatron.catalog.domain.CatalogEnums.Status;
import com.logatron.catalog.persistence.*;
import com.logatron.common.error.ApiException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;import java.util.*;import java.util.stream.Collectors;

@Service @Transactional(readOnly=true)
public class AuthorizationService {
    private final UserAccountRepository users;private final ProjectMembershipRepository memberships;private final EnvironmentGrantRepository grants;private final ProjectRepository projects;private final EnvironmentRepository environments;
    public AuthorizationService(UserAccountRepository users,ProjectMembershipRepository memberships,EnvironmentGrantRepository grants,ProjectRepository projects,EnvironmentRepository environments){this.users=users;this.memberships=memberships;this.grants=grants;this.projects=projects;this.environments=environments;}
    public AuthenticatedUser authenticatedUser(Jwt jwt){String issuer=jwt.getIssuer()==null?jwt.getClaimAsString("iss"):jwt.getIssuer().toString();var user=users.findByIssuerAndSubjectAndStatus(issuer,jwt.getSubject(),Status.ACTIVE).orElseThrow(ApiException::notFound);return new AuthenticatedUser(user.getId(),user.getIssuer(),user.getSubject(),user.getUsername(),user.getDisplayName(),user.getEmail());}
    public EffectiveScope resolveScope(Jwt jwt){return resolveScope(authenticatedUser(jwt));}
    public EffectiveScope resolveScope(AuthenticatedUser user){
        var active=memberships.findActiveDetailedForUser(user.id(),Instant.now());boolean admin=active.stream().anyMatch(m->m.getRole().getCode()==PlatformRole.ADMIN);
        if(admin){Map<UUID,EffectiveScope.ProjectScope> all=new LinkedHashMap<>();for(var p:projects.findAll()){Set<UUID> env=environments.findByProjectIdOrderByCode(p.getId()).stream().map(EnvironmentEntity::getId).collect(Collectors.toSet());all.put(p.getId(),new EffectiveScope.ProjectScope(p.getId(),Set.of(PlatformRole.ADMIN),env,Map.of(),true));}return new EffectiveScope(true,all);}
        List<UUID> ids=active.stream().map(ProjectMembershipEntity::getId).toList();var byMembership=ids.isEmpty()?Map.<UUID,List<EnvironmentGrantEntity>>of():grants.findDetailedByMembershipIds(ids).stream().collect(Collectors.groupingBy(g->g.getMembership().getId()));
        Map<UUID,List<ProjectMembershipEntity>> byProject=active.stream().collect(Collectors.groupingBy(m->m.getProject().getId(),LinkedHashMap::new,Collectors.toList()));Map<UUID,EffectiveScope.ProjectScope> result=new LinkedHashMap<>();
        byProject.forEach((projectId,ms)->{Set<PlatformRole> roles=ms.stream().map(m->m.getRole().getCode()).collect(Collectors.toSet());boolean allEnv=roles.contains(PlatformRole.PROJECT_ADMIN);Set<UUID> envIds=new HashSet<>();Map<UUID,Set<UUID>> serviceMap=new HashMap<>();if(allEnv)envIds.addAll(environments.findByProjectIdOrderByCode(projectId).stream().map(EnvironmentEntity::getId).toList());else for(var m:ms)for(var g:byMembership.getOrDefault(m.getId(),List.of())){UUID env=g.getEnvironment().getId();envIds.add(env);if(g.getService()!=null)serviceMap.computeIfAbsent(env,k->new HashSet<>()).add(g.getService().getId());else serviceMap.put(env,Set.of());}result.put(projectId,new EffectiveScope.ProjectScope(projectId,roles,envIds,serviceMap,allEnv));});
        return new EffectiveScope(false,result);
    }
    public EffectiveScope resolveScope(AuthenticatedUser user,RequestedResourceScope requested){EffectiveScope scope=resolveScope(user);require(scope,requested);return scope;}
    public void require(EffectiveScope scope,RequestedResourceScope requested){var ps=scope.projects().get(requested.projectId());if(ps==null)throw ApiException.denied();if(requested.environmentId()!=null&&!ps.environmentIds().contains(requested.environmentId()))throw ApiException.denied();if(requested.serviceId()!=null&&requested.environmentId()!=null){Set<UUID> allowed=ps.serviceIdsByEnvironment().get(requested.environmentId());if(allowed!=null&&!allowed.isEmpty()&&!allowed.contains(requested.serviceId()))throw ApiException.denied();}}
    public void requireAdministration(EffectiveScope scope,UUID projectId){var ps=scope.projects().get(projectId);if(ps==null||(!scope.globalAdmin()&&!ps.roles().contains(PlatformRole.PROJECT_ADMIN)))throw ApiException.denied();}
}

