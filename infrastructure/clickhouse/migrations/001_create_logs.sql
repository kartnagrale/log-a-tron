CREATE DATABASE IF NOT EXISTS logatron;

CREATE TABLE IF NOT EXISTS logatron.logs_local
(
 schema_version UInt16,
 event_id UUID,
 timestamp DateTime64(3, 'UTC') CODEC(Delta, ZSTD),
 observed_timestamp DateTime64(3, 'UTC') CODEC(Delta, ZSTD),
 ingested_at DateTime64(3, 'UTC') CODEC(Delta, ZSTD),
 company_id UUID, project_id UUID, environment_id UUID,
 server_id Nullable(UUID), service_id UUID, service_instance_id Nullable(UUID),
 company LowCardinality(String), project LowCardinality(String), environment LowCardinality(String),
 server LowCardinality(String), hostname LowCardinality(String), server_ip String,
 service LowCardinality(String), service_instance String, log_file String,
 log_type LowCardinality(String),
 level Enum8('UNKNOWN'=0,'TRACE'=1,'DEBUG'=2,'INFO'=3,'WARN'=4,'ERROR'=5,'FATAL'=6),
 severity_number UInt8,
 trace_id Nullable(String), span_id Nullable(String), trace_flags Nullable(String),
 request_id Nullable(String), correlation_id Nullable(String), transaction_id Nullable(String),
 order_token Nullable(String), auction_id Nullable(String), logger Nullable(String), thread Nullable(String),
 message String CODEC(ZSTD), exception_type Nullable(String), stack_trace Nullable(String) CODEC(ZSTD),
 attributes Map(String,String), resource_attributes Map(String,String),
 data_classification LowCardinality(String), masking_applied UInt8,
 fingerprint FixedString(64), collector_id LowCardinality(String), source_offset String,
 INDEX idx_trace trace_id TYPE bloom_filter(0.001) GRANULARITY 4,
 INDEX idx_correlation correlation_id TYPE bloom_filter(0.005) GRANULARITY 4,
 INDEX idx_request request_id TYPE bloom_filter(0.005) GRANULARITY 4,
 INDEX idx_order order_token TYPE bloom_filter(0.005) GRANULARITY 4,
 INDEX idx_exception exception_type TYPE set(1000) GRANULARITY 4,
 INDEX idx_message_tokens message TYPE tokenbf_v1(32768,3,0) GRANULARITY 8
)
ENGINE=ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(timestamp)
ORDER BY (project_id,environment_id,service_id,toDate(timestamp),level,timestamp,event_id)
TTL timestamp + INTERVAL 90 DAY DELETE
SETTINGS index_granularity=8192;

CREATE TABLE IF NOT EXISTS logatron.log_counts_1m
(
 bucket DateTime('UTC'), project_id UUID, environment_id UUID, service_id UUID,
 level UInt8, count AggregateFunction(count)
)
ENGINE=AggregatingMergeTree
PARTITION BY toYYYYMM(bucket)
ORDER BY (project_id,environment_id,service_id,level,bucket);

CREATE MATERIALIZED VIEW IF NOT EXISTS logatron.log_counts_1m_mv TO logatron.log_counts_1m AS
SELECT toStartOfMinute(timestamp) bucket,project_id,environment_id,service_id,toUInt8(level) level,countState() count
FROM logatron.logs_local GROUP BY bucket,project_id,environment_id,service_id,level;