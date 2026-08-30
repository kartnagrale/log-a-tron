package com.logatron.processor.configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
@ConfigurationProperties("logatron")
public record IngestionProperties(Ingestion ingestion, Masking masking, MetadataCache metadataCache, Topics kafka, ClickHouse clickhouse) {
 public record Ingestion(int maxEventBytes,int maxStackTraceBytes,int batchSize,int batchBytes,Duration flushInterval,Duration dedupWindow){}
 public record Masking(boolean enabled,String replacement){}
 public record MetadataCache(Duration ttl){}
 public record Topics(String raw,String enriched,String errors,String dlq){}
 public record ClickHouse(String url,String database,String user,String password,Duration requestTimeout){}
}