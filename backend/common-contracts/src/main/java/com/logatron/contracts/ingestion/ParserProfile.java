package com.logatron.contracts.ingestion;

import java.util.Locale;

public enum ParserProfile {
    JSON,
    PLAINTEXT,
    JAVA_PIPE_V1;

    public static ParserProfile fromExternalName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parser profile is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "json", "logback-json" -> JSON;
            case "plaintext", "plain-text" -> PLAINTEXT;
            case "java-pipe", "java-pipe-v1" -> JAVA_PIPE_V1;
            default -> throw new IllegalArgumentException("Unsupported parser profile: " + value);
        };
    }
}
