package com.logatron.agent.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("logatron.agent")
public record AgentManagementProperties(int pollSeconds) {
    public AgentManagementProperties {
        pollSeconds = pollSeconds <= 0 ? 15 : pollSeconds;
    }
}
