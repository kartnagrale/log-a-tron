package com.logatron.processor.parse;

import org.springframework.stereotype.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class TimestampNormalizer {
    private static final DateTimeFormatter JAVA = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss[.SSS]");

    public Instant parse(String value) {
        return parse(value, ZoneOffset.UTC);
    }

    public Instant parse(String value, ZoneId sourceZone) {
        if (value == null || value.isBlank()) return null;
        ZoneId zone = sourceZone == null ? ZoneOffset.UTC : sourceZone;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
        }
        return parseLocal(value, JAVA, zone);
    }

    public Instant parseLocal(String value, DateTimeFormatter formatter, ZoneId sourceZone) {
        if (value == null || value.isBlank()) return null;
        ZoneId zone = sourceZone == null ? ZoneOffset.UTC : sourceZone;
        try {
            return LocalDateTime.parse(value, formatter).atZone(zone).toInstant();
        } catch (DateTimeParseException e) {
            throw new ProcessingException("INVALID_TIMESTAMP", "Application timestamp is invalid");
        }
    }
}
