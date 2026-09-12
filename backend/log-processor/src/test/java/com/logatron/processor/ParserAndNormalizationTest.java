package com.logatron.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logatron.contracts.ingestion.ParserProfile;
import com.logatron.processor.parse.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.ZoneId;
import static org.assertj.core.api.Assertions.*;

class ParserAndNormalizationTest {
    @Test
    void parsesJsonAliasesUnicodeAndIdentifiers() {
        var p = new JsonLogParser(new ObjectMapper(), new TimestampNormalizer());
        var e = p.parse(TestFixtures.raw(ParserProfile.JSON,"{\"@timestamp\":\"2026-08-27T10:30:01.125Z\",\"severity\":\"ERROR\",\"message\":\"नमस्ते\",\"trace_id\":\"abc\",\"orderToken\":\"demo-order-42\"}"));
        assertThat(e.timestamp()).isEqualTo(Instant.parse("2026-08-27T10:30:01.125Z"));
        assertThat(e.message()).isEqualTo("नमस्ते");
        assertThat(e.traceId()).isEqualTo("abc");
        assertThat(e.orderToken()).isEqualTo("demo-order-42");
    }

    @Test
    void parsesPlaintextMultilineAsOneEvent() {
        String line = "2026-08-27 10:30:01.125 ERROR [worker-1] com.example.Service - Failed requestId=req-1\njava.sql.SQLTimeoutException: timeout\n    at com.example.Service.run(Service.java:1)";
        var e = new PlaintextLogParser(new TimestampNormalizer()).parse(TestFixtures.raw(ParserProfile.PLAINTEXT,line));
        assertThat(e.message()).contains("Failed");
        assertThat(e.requestId()).isEqualTo("req-1");
        assertThat(e.exceptionType()).isEqualTo("java.sql.SQLTimeoutException");
        assertThat(e.stackTrace()).contains("Service.java:1");
    }

    @Test
    void parsesJavaPipeFormatWithSourceTimezoneAndMessagePipes() {
        String line = "http-nio-8080-exec-286 |  INFO | 09 Sep 2026 17:59:56,059 | MasterDaoImpl.java:4723:getGroupCodeDetails | Query: select a | b from sample requestId=req-99";
        var parser = new JavaPipeLogParser(new TimestampNormalizer());
        var event = parser.parse(TestFixtures.raw(ParserProfile.JAVA_PIPE_V1,line), ZoneId.of("Asia/Kolkata"));

        assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-09-09T12:29:56.059Z"));
        assertThat(event.level()).isEqualTo("INFO");
        assertThat(event.thread()).isEqualTo("http-nio-8080-exec-286");
        assertThat(event.logger()).isEqualTo("MasterDaoImpl.java:4723:getGroupCodeDetails");
        assertThat(event.message()).contains("select a | b");
        assertThat(event.requestId()).isEqualTo("req-99");
        assertThat(event.attributes()).containsEntry("parser.profile","JAVA_PIPE_V1");
    }

    @Test
    void parsesLevelFirstJavaPipeFormatUsedByUtilityLogs() {
        String line = "  INFO | brokerpfmuat.neml.xyz-startStop-1 | 11 Sep 2026 08:40:17,929 | DBUtil.java:81 | Current Idle Persistence connections before acquiring 3 | pool=main";
        var event = new JavaPipeLevelFirstLogParser(new TimestampNormalizer()).parse(
                TestFixtures.raw(ParserProfile.JAVA_PIPE_LEVEL_FIRST_V1,line), ZoneId.of("Asia/Kolkata"));

        assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-09-11T03:10:17.929Z"));
        assertThat(event.level()).isEqualTo("INFO");
        assertThat(event.thread()).isEqualTo("brokerpfmuat.neml.xyz-startStop-1");
        assertThat(event.logger()).isEqualTo("DBUtil.java:81");
        assertThat(event.message()).contains("3 | pool=main");
        assertThat(event.attributes()).containsEntry("parser.profile","JAVA_PIPE_LEVEL_FIRST_V1");
    }

    @Test
    void parsesJavaPipeMultilineException() {
        String line = "http-nio-8080-exec-1 | ERROR | 09 Sep 2026 18:00:00,000 | ExampleService.java:42:run | Request failed\njava.sql.SQLTimeoutException: timeout\n    at com.example.ExampleService.run(ExampleService.java:42)";
        var event = new JavaPipeLogParser(new TimestampNormalizer()).parse(
                TestFixtures.raw(ParserProfile.JAVA_PIPE_V1,line), ZoneId.of("Asia/Kolkata"));

        assertThat(event.message()).isEqualTo("Request failed");
        assertThat(event.exceptionType()).isEqualTo("java.sql.SQLTimeoutException");
        assertThat(event.stackTrace()).contains("ExampleService.java:42");
    }

    @Test
    void sourceTimezoneIsUsedForNaiveTimestamps() {
        var normalizer = new TimestampNormalizer();
        assertThat(normalizer.parse("2026-09-09 17:59:56.059", ZoneId.of("Asia/Kolkata")))
                .isEqualTo(Instant.parse("2026-09-09T12:29:56.059Z"));
    }

    @Test
    void parserProfilesUseExplicitCompatibleNames() {
        assertThat(ParserProfile.fromExternalName("logback-json")).isEqualTo(ParserProfile.JSON);
        assertThat(ParserProfile.fromExternalName("plaintext")).isEqualTo(ParserProfile.PLAINTEXT);
        assertThat(ParserProfile.fromExternalName("java-pipe-v1")).isEqualTo(ParserProfile.JAVA_PIPE_V1);
        assertThat(ParserProfile.fromExternalName("java-pipe-level-first-v1")).isEqualTo(ParserProfile.JAVA_PIPE_LEVEL_FIRST_V1);
        assertThatThrownBy(() -> ParserProfile.fromExternalName("some-project-special-parser"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedJsonFailsExplicitly() {
        assertThatThrownBy(() -> new JsonLogParser(new ObjectMapper(),new TimestampNormalizer()).parse(TestFixtures.raw(ParserProfile.JSON,"{bad")))
                .isInstanceOf(ProcessingException.class)
                .hasMessageContaining("cannot be parsed");
    }

    @Test
    void invalidTimestampIsNotSilentlyReplaced() {
        assertThatThrownBy(() -> new TimestampNormalizer().parse("not-time"))
                .isInstanceOf(ProcessingException.class);
    }

    @Test
    void severityAliasesNormalize() {
        var s = new SeverityNormalizer();
        assertThat(s.normalize("WARNING")).isEqualTo("WARN");
        assertThat(s.normalize("ERR")).isEqualTo("ERROR");
        assertThat(s.normalize("CRITICAL")).isEqualTo("FATAL");
        assertThat(s.normalize("odd")).isEqualTo("UNKNOWN");
    }
}
