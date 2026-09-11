package com.logatron.agent.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("logatron.agent")
public record AgentManagementProperties(
        int pollSeconds,
        String stateDirectory) {

    public AgentManagementProperties {
        pollSeconds = pollSeconds <= 0 ? 15 : pollSeconds;
        stateDirectory = stateDirectory == null || stateDirectory.isBlank() ? "/var/lib/logatron-agent" : stateDirectory.trim();
    }
}
