package com.logatron.auth.web;
import com.logatron.auth.application.MeService;import com.logatron.auth.domain.PlatformRole;import io.swagger.v3.oas.annotations.Operation;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1") public class MeController{
 private final MeService service;public MeController(MeService service){this.service=service;}
 @GetMapping("/me") @Operation(summary="Return the current user and effective authorized scope") public MeResponse me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt){return service.me(jwt);}
 public record MeResponse(UserView user,Set<PlatformRole> roles,List<ProjectAccess> projects){} public record UserView(UUID id,String username,String displayName,String email){} public record ProjectAccess(UUID projectId,String projectName,Set<PlatformRole> roles,List<EnvironmentAccess> environments){} public record EnvironmentAccess(UUID environmentId,String code,String name){}
}
