package com.logatron.agent.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("logatron.agent")
public record AgentManagementProperties(
        String gatewayEndpoint,
        boolean gatewayInsecure,
        int pollSeconds,
        String stateDirectory) {

    public AgentManagementProperties {
        gatewayEndpoint = gatewayEndpoint == null || gatewayEndpoint.isBlank() ? "localhost:4317" : gatewayEndpoint.trim();
        pollSeconds = pollSeconds <= 0 ? 15 : pollSeconds;
        stateDirectory = stateDirectory == null || stateDirectory.isBlank() ? "/var/lib/logatron-agent" : stateDirectory.trim();
    }
}
