package com.logatron.auth.dev;
import com.nimbusds.jose.jwk.source.ImmutableSecret;import javax.crypto.SecretKey;import javax.crypto.spec.SecretKeySpec;import org.springframework.context.annotation.*;import org.springframework.security.oauth2.core.*;import org.springframework.security.oauth2.jwt.*;import java.nio.charset.StandardCharsets;
@Configuration @Profile("dev-auth") public class DevAuthConfiguration{
 @Bean JwtDecoder jwtDecoder(DevAuthProperties p){SecretKey key=key(p);NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key).build();OAuth2TokenValidator<Jwt> issuer=JwtValidators.createDefaultWithIssuer(p.issuer());OAuth2TokenValidator<Jwt> audience=jwt->jwt.getAudience().contains(p.audience())?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Required audience is missing",null));decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer,audience));return decoder;}
 @Bean JwtEncoder jwtEncoder(DevAuthProperties p){return new NimbusJwtEncoder(new ImmutableSecret<>(key(p)));}
 private SecretKey key(DevAuthProperties p){if(p.secret()==null||p.secret().getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalStateException("DEV_JWT_SECRET must contain at least 32 bytes");return new SecretKeySpec(p.secret().getBytes(StandardCharsets.UTF_8),"HmacSHA256");}
}

