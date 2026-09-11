package com.logatron.agent.web;

import com.logatron.agent.application.CollectorAgentService;
import com.logatron.agent.web.CollectorAgentDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class CollectorAgentAdminController {
    private final CollectorAgentService service;
    public CollectorAgentAdminController(CollectorAgentService service){this.service=service;}

    @PostMapping("/collector-agents")
    public AgentRegistration register(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody RegisterAgentRequest body,HttpServletRequest request){return service.register(jwt,body,request);}

    @GetMapping("/servers/{serverId}/collector-agent")
    public ResponseEntity<AgentView> forServer(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID serverId){AgentView view=service.getForServer(jwt,serverId);return view==null?ResponseEntity.noContent().build():ResponseEntity.ok(view);}

    @PostMapping("/collector-agents/{agentId}/rotate-token")
    public AgentRegistration rotate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID agentId,HttpServletRequest request){return service.rotateToken(jwt,agentId,request);}
}
