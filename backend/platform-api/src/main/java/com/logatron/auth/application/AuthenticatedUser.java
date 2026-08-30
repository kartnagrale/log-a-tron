package com.logatron.auth.application;
import java.util.UUID;
public record AuthenticatedUser(UUID id,String issuer,String subject,String username,String displayName,String email){}

