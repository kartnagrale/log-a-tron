package com.logatron.processor.parse;

import com.logatron.contracts.ingestion.ParserProfile;
import com.logatron.contracts.ingestion.RawLogEnvelope;
import org.springframework.stereotype.Component;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reusable parser for Java application logs shaped as:
 * thread | LEVEL | dd MMM uuuu HH:mm:ss,SSS | source-location | message
 *
 * The profile is format-specific, never project-specific. The fifth split is
 * intentionally unbounded so pipe characters inside messages are preserved.
 */
@Component
public class JavaPipeLogParser implements LogParser {
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm:ss,SSS", Locale.ENGLISH);
    private static final Pattern ID = Pattern.compile(
            "\\b(traceId|spanId|requestId|correlationId|transactionId|orderToken|auctionId)=([A-Za-z0-9._:-]+)");
    private static final Pattern EXCEPTION = Pattern.compile(
            "(?m)^\\s*([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*(?:Exception|Error))(?::|\\s|$)");

    private final TimestampNormalizer time;

    public JavaPipeLogParser(TimestampNormalizer time) {
        this.time = time;
    }

    @Override
    public boolean supports(ParserProfile profile) {
        return profile == ParserProfile.JAVA_PIPE_V1;
    }

    @Override
    public ParsedLog parse(RawLogEnvelope envelope, ZoneId sourceZone) {
        String payload = envelope.payload();
        if (payload == null || payload.isBlank()) {
            throw new ProcessingException("EMPTY_MESSAGE", "Java pipe payload is empty");
        }

        String[] parts = payload.split("\\|", 5);
        if (parts.length != 5) {
            throw new ProcessingException(
                    "MALFORMED_JAVA_PIPE",
                    "Java pipe payload must contain thread, level, timestamp, source and message fields");
        }

        String thread = clean(parts[0]);
        String level = clean(parts[1]);
        String timestamp = clean(parts[2]);
        String sourceLocation = clean(parts[3]);
        String messageBlock = clean(parts[4]);

        if (thread.isBlank() || level.isBlank() || timestamp.isBlank() || sourceLocation.isBlank()) {
            throw new ProcessingException("MALFORMED_JAVA_PIPE", "Java pipe payload contains an empty required field");
        }
        if (messageBlock.isBlank()) {
            throw new ProcessingException("EMPTY_MESSAGE", "Java pipe message is required");
        }

        Map<String, String> ids = new HashMap<>();
        Matcher idMatcher = ID.matcher(messageBlock);
        while (idMatcher.find()) ids.put(idMatcher.group(1), idMatcher.group(2));

        String exceptionType = exceptionType(messageBlock);
        String stackTrace = null;
        String message = messageBlock;
        int firstLineBreak = firstLineBreak(messageBlock);
        if (exceptionType != null && firstLineBreak >= 0) {
            message = messageBlock.substring(0, firstLineBreak).strip();
            stackTrace = messageBlock.substring(skipLineBreak(messageBlock, firstLineBreak)).strip();
        }

        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("source.location", sourceLocation);
        attributes.put("parser.profile", "JAVA_PIPE_V1");

        return new ParsedLog(
                time.parseLocal(timestamp, TIMESTAMP, sourceZone),
                level,
                sourceLocation,
                thread,
                message,
                exceptionType,
                stackTrace,
                ids.get("traceId"),
                ids.get("spanId"),
                null,
                ids.get("requestId"),
                ids.get("correlationId"),
                ids.get("transactionId"),
                ids.get("orderToken"),
                ids.get("auctionId"),
                Map.copyOf(attributes));
    }

    private static String clean(String value) {
        return value.replace('\u00A0', ' ').trim();
    }

    private static String exceptionType(String value) {
        Matcher matcher = EXCEPTION.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static int firstLineBreak(String value) {
        int cr = value.indexOf('\r');
        int lf = value.indexOf('\n');
        if (cr < 0) return lf;
        if (lf < 0) return cr;
        return Math.min(cr, lf);
    }

    private static int skipLineBreak(String value, int index) {
        if (value.charAt(index) == '\r' && index + 1 < value.length() && value.charAt(index + 1) == '\n') {
            return index + 2;
        }
        return index + 1;
    }
}
