package com.logatron.catalog.web;
import com.logatron.catalog.application.CatalogQueryService;import com.logatron.catalog.web.CatalogDtos.*;import io.swagger.v3.oas.annotations.Operation;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1") public class CatalogController{private final CatalogQueryService service;public CatalogController(CatalogQueryService service){this.service=service;}
 @GetMapping("/projects") @Operation(summary="List projects in the effective authorized scope") public List<ProjectView> projects(@AuthenticationPrincipal Jwt jwt){return service.projects(jwt);}
 @GetMapping("/projects/{id}") public ProjectView project(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.project(jwt,id);}
 @GetMapping("/projects/{id}/environments") public List<EnvironmentView> environments(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.environments(jwt,id);}
 @GetMapping("/environments/{id}/servers") public List<ServerView> servers(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.servers(jwt,id);}
 @GetMapping("/projects/{id}/services") public List<ServiceView> services(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.services(jwt,id);}
 @GetMapping("/servers/{id}/service-instances") public List<ServiceInstanceView> instances(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.instances(jwt,id);}
 @GetMapping("/service-instances/{id}/log-sources") public List<LogSourceView> sources(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.logSources(jwt,id);}
}
