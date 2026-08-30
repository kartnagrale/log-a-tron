package com.logatron.auth.dev;
import io.swagger.v3.oas.annotations.Operation;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.context.annotation.Profile;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/v1/dev-auth") @Profile("dev-auth") public class DevTokenController{
 private final DevTokenService service;public DevTokenController(DevTokenService service){this.service=service;}
 @GetMapping("/users") @Operation(summary="List fixed local development identities") public List<String> users(){return service.users();}
 @PostMapping("/token") @Operation(summary="Issue a local-only signed development JWT") public TokenResponse token(@Valid @RequestBody TokenRequest request){return service.token(request.username());}
 public record TokenRequest(@NotBlank @Size(max=120) String username){}public record TokenResponse(String accessToken,String tokenType,long expiresIn){}
}
