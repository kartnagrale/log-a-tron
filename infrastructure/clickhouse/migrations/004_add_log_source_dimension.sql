ALTER TABLE logatron.logs_local
    ADD COLUMN IF NOT EXISTS log_source_id Nullable(UUID) AFTER service_instance_id;

ALTER TABLE logatron.logs_local
    ADD INDEX IF NOT EXISTS idx_log_source log_source_id TYPE set(10000) GRANULARITY 4;
