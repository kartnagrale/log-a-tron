package com.logatron.tracequery.configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;import java.time.Duration;
@ConfigurationProperties("logatron.trace-query") public record TraceQueryProperties(String tempoUrl,Duration timeout){public TraceQueryProperties{if(timeout==null)timeout=Duration.ofSeconds(5);}}
