package com.logatron.agent.application;

import com.logatron.agent.configuration.AgentManagementProperties;
import com.logatron.agent.configuration.ShipperManagementProperties;
import com.logatron.agent.persistence.*;
import com.logatron.agent.web.CollectorAgentDtos.*;
import com.logatron.audit.application.AuditService;
import com.logatron.auth.application.*;
import com.logatron.catalog.domain.CatalogEnums.CollectorStatus;
import com.logatron.catalog.persistence.*;
import com.logatron.common.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
public class CollectorAgentService {
    private final CollectorAgentRepository agents;
    private final CollectorConfigRevisionRepository revisions;
    private final ServerRepository servers;
    private final AuthorizationService authorization;
    private final AuditService audit;
    private final CollectorConfigCompiler compiler;
    private final AgentManagementProperties properties;
    private final ShipperManagementProperties settings;
    private final CollectorDeploymentRepository deployments;
    private final SecureRandom random=new SecureRandom();

    public CollectorAgentService(CollectorAgentRepository agents,CollectorConfigRevisionRepository revisions,ServerRepository servers,AuthorizationService authorization,AuditService audit,CollectorConfigCompiler compiler,AgentManagementProperties properties,ShipperManagementProperties settings,CollectorDeploymentRepository deployments){
        this.agents=agents;this.revisions=revisions;this.servers=servers;this.authorization=authorization;this.audit=audit;this.compiler=compiler;this.properties=properties;this.settings=settings;this.deployments=deployments;
    }

    @Transactional
    public AgentRegistration register(Jwt jwt,RegisterAgentRequest request,HttpServletRequest http){
        ServerEntity server=servers.findById(request.serverId()).orElseThrow(ApiException::notFound);AuthenticatedUser actor=admin(jwt,server);
        settings.require(request.gatewayEndpoint().trim(),request.gatewayInsecure(),server.getEnvironment().getProject().getId(),server.getEnvironment().getId());
        if(agents.findByServer_Id(server.getId()).isPresent())throw ApiException.invalid("A managed collector agent is already registered for this server.");
        if(agents.findByAgentKey(request.agentKey()).isPresent())throw ApiException.invalid("Agent key is already in use.");
        String token=newToken();CollectorAgentEntity entity=new CollectorAgentEntity(UUID.randomUUID(),server,request.agentKey().trim(),hash(token),request.gatewayEndpoint().trim(),request.gatewayInsecure());agents.save(entity);
        audit.record(actor,"COLLECTOR_AGENT_REGISTER","COLLECTOR_AGENT",entity.getId(),server.getEnvironment().getProject().getId(),server.getEnvironment().getId(),"SUCCESS",http);
        return new AgentRegistration(view(entity),token);
    }

    @Transactional
    public AgentView getForServer(Jwt jwt,UUID serverId){
        ServerEntity server=servers.findById(serverId).orElseThrow(ApiException::notFound);admin(jwt,server);return agents.findByServer_Id(serverId).map(this::view).orElse(null);
    }

    @Transactional
    public AgentRegistration rotateToken(Jwt jwt,UUID agentId,HttpServletRequest http){
        CollectorAgentEntity entity=agents.findById(agentId).orElseThrow(ApiException::notFound);AuthenticatedUser actor=admin(jwt,entity.getServer());String token=newToken();entity.rotateToken(hash(token));
        audit.record(actor,"COLLECTOR_AGENT_TOKEN_ROTATE","COLLECTOR_AGENT",entity.getId(),entity.getServer().getEnvironment().getProject().getId(),entity.getServer().getEnvironment().getId(),"SUCCESS",http);
        return new AgentRegistration(view(entity),token);
    }

    @Transactional
    public AgentView updateGateway(Jwt jwt,UUID agentId,UpdateAgentGatewayRequest request,HttpServletRequest http){
        CollectorAgentEntity entity=agents.findById(agentId).orElseThrow(ApiException::notFound);AuthenticatedUser actor=admin(jwt,entity.getServer());settings.require(request.gatewayEndpoint().trim(),request.gatewayInsecure(),entity.getServer().getEnvironment().getProject().getId(),entity.getServer().getEnvironment().getId());entity.updateGateway(request.gatewayEndpoint().trim(),request.gatewayInsecure());
        audit.record(actor,"COLLECTOR_AGENT_GATEWAY_UPDATE","COLLECTOR_AGENT",entity.getId(),entity.getServer().getEnvironment().getProject().getId(),entity.getServer().getEnvironment().getId(),"SUCCESS",http);
        return view(entity);
    }

    @Transactional
    public ConfigDelivery config(UUID agentId,String token){
        CollectorAgentEntity agent=authenticate(agentId,token);
        if(!"managed-v2".equals(agent.getDeploymentProtocol())||agent.getSelectedConfigVersion()==null)return null;
        CollectorConfigRevisionEntity revision=revisions.findByAgent_IdAndConfigVersion(agentId,agent.getSelectedConfigVersion()).orElseThrow(ApiException::notFound);
        return new ConfigDelivery(revision.getConfigYaml(),revision.getConfigHash(),revision.getSourceCount(),revision.getConfigVersion(),deployments.findByAgent_IdAndStatus(agentId,"PENDING").map(CollectorDeploymentEntity::getId).orElse(null));
    }

    @Transactional
    public void heartbeat(UUID agentId,String token,AgentHeartbeatRequest request){
        CollectorAgentEntity entity=authenticate(agentId,token);Instant now=Instant.now();CollectorStatus status=request.status()==null?CollectorStatus.ONLINE:request.status();
        if(status==CollectorStatus.UNKNOWN||status==CollectorStatus.OFFLINE)status=CollectorStatus.ONLINE;
        entity.deploymentProtocol(request.protocol());
        deployments.findByAgent_IdAndStatus(agentId,"PENDING").ifPresent(d->{
            boolean matches=Objects.equals(request.deploymentId(),d.getId());
            if(matches&&request.lastError()!=null&&!request.lastError().isBlank()){
                d.complete("FAILED",request.lastError());entity.selectConfiguration(d.getPreviousVersion());
            }else if(matches&&request.healthVerified()&&request.status()==CollectorStatus.ONLINE&&Objects.equals(request.appliedConfigVersion(),d.getRevision().getConfigVersion())&&Objects.equals(request.appliedConfigHash(),d.getRevision().getConfigHash())){
                d.complete("APPLIED","Collector health verified by agent; ingestion verification is unknown.");
            }
        });
        entity.heartbeat(status,request.collectorVersion(),request.appliedConfigHash(),request.appliedConfigVersion(),request.lastError(),now);entity.getServer().collectorHeartbeat(status,now);
    }

    @Transactional
    public AgentView viewForAgent(UUID agentId,String token){return view(authenticate(agentId,token));}

    @Transactional
    public List<ConfigRevisionView> revisions(Jwt jwt,UUID agentId){
        CollectorAgentEntity entity=agents.findById(agentId).orElseThrow(ApiException::notFound);admin(jwt,entity.getServer());
        return revisions.findByAgent_IdOrderByConfigVersionDesc(agentId).stream()
                .map(r->new ConfigRevisionView(r.getConfigVersion(),r.getConfigHash(),r.getSourceCount(),r.getCreatedAt(),Objects.equals(entity.getAppliedConfigVersion(),r.getConfigVersion())))
                .toList();
    }

    public int pollSeconds(){return properties.pollSeconds();}

    private CollectorAgentEntity authenticate(UUID id,String token){
        CollectorAgentEntity entity=agents.lockById(id).orElseThrow(ApiException::notFound);if(token==null||token.isBlank()||!constantEquals(entity.getTokenHash(),hash(token)))throw ApiException.notFound();return entity;
    }

    private AgentView view(CollectorAgentEntity entity){
        var latest=revisions.findTopByAgent_IdOrderByConfigVersionDesc(entity.getId());
        Instant seen=entity.getLastSeenAt();CollectorStatus effective=seen==null?CollectorStatus.UNKNOWN:seen.isBefore(Instant.now().minusSeconds(Math.max(45,properties.pollSeconds()*4L)))?CollectorStatus.OFFLINE:entity.getStatus();String applied=entity.getAppliedConfigHash();
        return new AgentView(entity.getId(),entity.getServer().getId(),entity.getAgentKey(),entity.getGatewayEndpoint(),entity.isGatewayInsecure(),effective,seen,entity.getCollectorVersion(),latest.map(CollectorConfigRevisionEntity::getConfigVersion).orElse(0L),latest.map(CollectorConfigRevisionEntity::getConfigHash).orElse(""),entity.getAppliedConfigVersion(),applied,latest.map(r->r.getConfigHash().equals(applied)).orElse(false),latest.map(CollectorConfigRevisionEntity::getSourceCount).orElse(0),entity.getLastError());
    }

    private AuthenticatedUser admin(Jwt jwt,ServerEntity server){AuthenticatedUser actor=authorization.authenticatedUser(jwt);authorization.requireAdministration(authorization.resolveScope(actor),server.getEnvironment().getProject().getId());return actor;}
    private String newToken(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("SHA-256 is unavailable",e);}}
    private static boolean constantEquals(String left,String right){return MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII),right.getBytes(StandardCharsets.US_ASCII));}
    public record ConfigDelivery(String yaml,String sha256,int sourceCount,long configVersion,UUID deploymentId){}
}
