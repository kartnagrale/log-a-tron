package com.logatron.agent.web;

import com.logatron.agent.application.ShipperConfigurationService;
import com.logatron.agent.web.ShipperDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/admin")
public class ShipperConfigurationController {
    private final ShipperConfigurationService service;
    public ShipperConfigurationController(ShipperConfigurationService service){this.service=service;}
    @GetMapping("/servers/{serverId}/shipper-destinations") public List<DestinationView> destinations(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID serverId){return service.destinations(jwt,serverId);}
    @PostMapping("/shipper-configurations/generate") public RevisionView generate(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody GenerateRequest body,HttpServletRequest http){return service.generate(jwt,body.agentId(),http);}
    @GetMapping("/collector-agents/{agentId}/shipper") public ShipperView history(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID agentId){return service.history(jwt,agentId);}
    @GetMapping("/collector-agents/{agentId}/config-revisions/{version}") public RevisionView get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID agentId,@PathVariable long version){return service.get(jwt,agentId,version);}
    @PostMapping("/collector-agents/{agentId}/config-revisions/{version}/validate") public RevisionView validate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID agentId,@PathVariable long version,HttpServletRequest http){return service.validate(jwt,agentId,version,http);}
    @GetMapping("/collector-agents/{agentId}/config-revisions/{version}/download") public ResponseEntity<String> download(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID agentId,@PathVariable long version,HttpServletRequest http){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"config.yml\"").header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType("application/yaml")).body(service.download(jwt,agentId,version,http).yaml());}
    @PutMapping("/collector-agents/{agentId}/deployment-settings") @ResponseStatus(HttpStatus.NO_CONTENT) public void settings(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID agentId,@Valid @RequestBody DeploymentSettings body,HttpServletRequest http){service.deploymentSettings(jwt,agentId,body,http);}
    @PostMapping("/collector-agents/{agentId}/deployments") public DeploymentView deploy(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID agentId,@Valid @RequestBody DeployRequest body,HttpServletRequest http){return service.deploy(jwt,agentId,body,"DEPLOY",http);}
    @PostMapping("/collector-agents/{agentId}/rollback") public DeploymentView rollback(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID agentId,@Valid @RequestBody DeployRequest body,HttpServletRequest http){return service.deploy(jwt,agentId,body,"ROLLBACK",http);}
}
