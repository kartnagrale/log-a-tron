package com.logatron.auth.dev;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("logatron.dev-auth") public record DevAuthProperties(String issuer,String audience,String secret,long tokenMinutes){}

