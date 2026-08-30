package com.logatron.administration.web;
import com.logatron.auth.domain.PlatformRole;import jakarta.validation.constraints.*;import java.time.Instant;import java.util.UUID;
public final class AdminDtos{private AdminDtos(){}public record CreatedResource(UUID id){}public record CreateMembershipRequest(@NotNull UUID userId,@NotNull UUID projectId,@NotNull PlatformRole role,Instant validUntil){}public record CreateGrantRequest(@NotNull UUID membershipId,@NotNull UUID environmentId,UUID serviceId){}public record AccessView(UUID membershipId,UUID userId,String user,String displayName,UUID projectId,String project,PlatformRole role,Instant validFrom,Instant validUntil){}
}
