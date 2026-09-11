package com.logatron.agent.web;

import com.logatron.agent.application.CollectorAgentService;
import com.logatron.agent.web.CollectorAgentDtos.AgentHeartbeatRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/v1/agents")
public class CollectorAgentInternalController {
    public static final String TOKEN_HEADER="X-Logatron-Agent-Token";
    private final CollectorAgentService service;
    public CollectorAgentInternalController(CollectorAgentService service){this.service=service;}

    @GetMapping(value="/{agentId}/config",produces="application/yaml")
    public ResponseEntity<String> config(@PathVariable UUID agentId,@RequestHeader(TOKEN_HEADER) String token){
        var compiled=service.config(agentId,token);return ResponseEntity.ok()
                .header("ETag","\""+compiled.sha256()+"\"")
                .header("X-Logatron-Config-Hash",compiled.sha256())
                .header("X-Logatron-Source-Count",String.valueOf(compiled.sourceCount()))
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(compiled.yaml());
    }

    @PostMapping("/{agentId}/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@PathVariable UUID agentId,@RequestHeader(TOKEN_HEADER) String token,@Valid @RequestBody AgentHeartbeatRequest body){service.heartbeat(agentId,token,body);}
}
