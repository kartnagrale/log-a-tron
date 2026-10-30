package com.logatron.agent.application;

import com.logatron.agent.configuration.*;
import com.logatron.agent.persistence.*;
import com.logatron.agent.web.ShipperDtos.*;
import com.logatron.audit.application.AuditService;
import com.logatron.auth.application.*;
import com.logatron.catalog.domain.CatalogEnums.Status;
import com.logatron.catalog.persistence.*;
import com.logatron.common.error.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.yaml.snakeyaml.Yaml;
import java.time.Instant;
import java.util.*;

@Service
public class ShipperConfigurationService {
    private final CollectorAgentRepository agents;
    private final CollectorConfigRevisionRepository revisions;
    private final CollectorDeploymentRepository deployments;
    private final ServerRepository servers;
    private final AuthorizationService auth;
    private final AuditService audit;
    private final CollectorConfigCompiler compiler;
    private final CollectorConfigurationValidator validator;
    private final ShipperManagementProperties settings;
    public ShipperConfigurationService(CollectorAgentRepository agents,CollectorConfigRevisionRepository revisions,CollectorDeploymentRepository deployments,ServerRepository servers,AuthorizationService auth,AuditService audit,CollectorConfigCompiler compiler,CollectorConfigurationValidator validator,ShipperManagementProperties settings){this.agents=agents;this.revisions=revisions;this.deployments=deployments;this.servers=servers;this.auth=auth;this.audit=audit;this.compiler=compiler;this.validator=validator;this.settings=settings;}

    @Transactional(readOnly=true) public List<DestinationView> destinations(Jwt jwt,UUID serverId){
        var server=servers.findById(serverId).orElseThrow(ApiException::notFound);admin(jwt,server);
        return settings.destinations().stream().filter(d->d.permits(server.getEnvironment().getProject().getId(),server.getEnvironment().getId())).map(d->new DestinationView(d.id(),d.name(),d.endpoint(),d.insecure())).toList();
    }
    @Transactional public RevisionView generate(Jwt jwt,UUID agentId,HttpServletRequest http){
        var agent=locked(agentId);var actor=admin(jwt,agent.getServer());destination(agent);
        var compiled=compiler.compile(agent);
        var revision=revisions.findByAgent_IdAndConfigHash(agentId,compiled.sha256()).orElseGet(()->{
            long version=revisions.findTopByAgent_IdOrderByConfigVersionDesc(agentId).map(r->r.getConfigVersion()+1).orElse(1L);
            var created=new CollectorConfigRevisionEntity(UUID.randomUUID(),agent,version,compiled.sha256(),compiled.yaml(),compiled.sourceCount(),Instant.now());created.warnings(compiled.warnings());return revisions.save(created);
        });
        record(actor,"SHIPPER_GENERATE",agent,revision.getId(),http);return view(revision);
    }
    @Transactional public RevisionView validate(Jwt jwt,UUID agentId,long version,HttpServletRequest http){
        var agent=locked(agentId);var actor=admin(jwt,agent.getServer());var revision=revision(agentId,version);
        var result=validator.validate(revision.getConfigYaml());revision.validation(result.status(),result.message());
        record(actor,"SHIPPER_VALIDATE",agent,revision.getId(),http);return view(revision);
    }
    @Transactional(readOnly=true) public RevisionView get(Jwt jwt,UUID agentId,long version){var agent=agents.findById(agentId).orElseThrow(ApiException::notFound);admin(jwt,agent.getServer());return view(revision(agentId,version));}
    @Transactional public RevisionView download(Jwt jwt,UUID agentId,long version,HttpServletRequest http){var agent=locked(agentId);var actor=admin(jwt,agent.getServer());var revision=revision(agentId,version);record(actor,"SHIPPER_DOWNLOAD",agent,revision.getId(),http);return view(revision);}
    @Transactional(readOnly=true) public ShipperView history(Jwt jwt,UUID agentId){
        var agent=agents.findById(agentId).orElseThrow(ApiException::notFound);admin(jwt,agent.getServer());String reason=deploymentReason(agent);
        return new ShipperView(agentId,agent.getServer().getId(),agent.getSelectedConfigVersion(),agent.isDeploymentEnabled(),reason==null,reason,agent.getDeploymentProtocol(),null,revisions.findByAgent_IdOrderByConfigVersionDesc(agentId).stream().map(this::view).toList(),deployments.findTop100ByAgent_IdOrderByRequestedAtDesc(agentId).stream().map(this::view).toList());
    }
    @Transactional public void deploymentSettings(Jwt jwt,UUID agentId,DeploymentSettings request,HttpServletRequest http){
        var agent=locked(agentId);var actor=admin(jwt,agent.getServer());if(!request.confirmed())throw ApiException.invalid("Explicit confirmation is required to change deployment authorization.");
        if(deployments.findByAgent_IdAndStatus(agentId,"PENDING").isPresent())throw conflict("A rollout is pending. Wait for its agent acknowledgement before changing deployment authorization.");
        agent.deploymentEnabled(request.enabled());record(actor,request.enabled()?"SHIPPER_DEPLOYMENT_ENABLE":"SHIPPER_DEPLOYMENT_DISABLE",agent,agentId,http);
    }
    @Transactional public DeploymentView deploy(Jwt jwt,UUID agentId,DeployRequest request,String action,HttpServletRequest http){
        var agent=locked(agentId);var actor=admin(jwt,agent.getServer());if(!request.confirmed())throw ApiException.invalid("Confirm the reviewed configuration and collector restart before deployment.");
        if(!Objects.equals(request.expectedSelectedVersion(),agent.getSelectedConfigVersion()))throw conflict("The selected configuration changed. Refresh and review the diff again.");
        String reason=deploymentReason(agent);if(reason!=null)throw ApiException.invalid(reason);
        var revision=revision(agentId,request.version());if(!"VALID".equals(revision.getValidationStatus()))throw ApiException.invalid("Validate this immutable configuration with contrib 0.135.0 before deployment.");
        requireStoredDestination(agent,revision);
        var pending=deployments.findByAgent_IdAndStatus(agentId,"PENDING");
        if(pending.isPresent()){
            if(pending.get().getRevision().getId().equals(revision.getId()))return view(pending.get());
            throw conflict("Another rollout is pending. Wait for acknowledgement before replacing it.");
        }
        if(Objects.equals(agent.getSelectedConfigVersion(),request.version()))throw conflict("This version is already selected.");
        var deployment=deployments.save(new CollectorDeploymentEntity(agent,revision,agent.getSelectedConfigVersion(),action,actor.id()));agent.selectConfiguration(request.version());
        record(actor,"SHIPPER_"+action,agent,deployment.getId(),http);return view(deployment);
    }
    private void requireStoredDestination(CollectorAgentEntity agent,CollectorConfigRevisionEntity revision){
        Map<String,Object> doc=new Yaml().load(revision.getConfigYaml());var exporters=(Map<?,?>)doc.get("exporters");var exporter=(Map<?,?>)exporters.get("otlp/logatron");var tls=(Map<?,?>)exporter.get("tls");
        settings.require(exporter.get("endpoint").toString(),Boolean.TRUE.equals(tls.get("insecure")),agent.getServer().getEnvironment().getProject().getId(),agent.getServer().getEnvironment().getId());
    }
    private String deploymentReason(CollectorAgentEntity agent){
        if(!agent.isDeploymentEnabled())return "Automated deployment is disabled for this server. Download remains available.";
        if(!"managed-v2".equals(agent.getDeploymentProtocol()))return "Install the managed-v2 pull agent before enabling automated rollout.";
        if(agent.getLastSeenAt()==null||agent.getLastSeenAt().isBefore(Instant.now().minusSeconds(60)))return "Agent heartbeat is missing or stale.";
        if(agent.getCollectorVersion()==null||!agent.getCollectorVersion().matches("(?s).*otelcol-contrib.*\\b0\\.135\\.0\\b.*"))return "Target must report otelcol-contrib 0.135.0.";
        if(agent.getServer().getStatus()==Status.OFFLINE)return "The server is decommissioned.";
        return null;
    }
    private void destination(CollectorAgentEntity a){settings.require(a.getGatewayEndpoint(),a.isGatewayInsecure(),a.getServer().getEnvironment().getProject().getId(),a.getServer().getEnvironment().getId());}
    private CollectorAgentEntity locked(UUID id){return agents.lockById(id).orElseThrow(ApiException::notFound);}
    private CollectorConfigRevisionEntity revision(UUID agent,long version){return revisions.findByAgent_IdAndConfigVersion(agent,version).orElseThrow(ApiException::notFound);}
    private AuthenticatedUser admin(Jwt jwt,ServerEntity server){var actor=auth.authenticatedUser(jwt);var scope=auth.resolveScope(actor);auth.requireAdministration(scope,server.getEnvironment().getProject().getId());auth.require(scope,RequestedResourceScope.environment(server.getEnvironment().getProject().getId(),server.getEnvironment().getId()));return actor;}
    private void record(AuthenticatedUser actor,String action,CollectorAgentEntity agent,UUID id,HttpServletRequest http){audit.record(actor,action,"COLLECTOR_CONFIGURATION",id,agent.getServer().getEnvironment().getProject().getId(),agent.getServer().getEnvironment().getId(),"SUCCESS",http);}
    private RevisionView view(CollectorConfigRevisionEntity r){return new RevisionView(r.getId(),r.getConfigVersion(),r.getConfigHash(),r.getSourceCount(),r.getCreatedAt(),r.getValidationStatus(),r.getValidationMessage(),r.getConfigYaml(),r.getWarnings());}
    private DeploymentView view(CollectorDeploymentEntity d){return new DeploymentView(d.getId(),d.getRevision().getConfigVersion(),d.getPreviousVersion(),d.getAction(),d.getStatus(),d.getRequestedAt(),d.getCompletedAt(),d.getLastError());}
    private static ApiException conflict(String message){return new ApiException(HttpStatus.CONFLICT,ErrorCode.CONFLICT,message);}
}
