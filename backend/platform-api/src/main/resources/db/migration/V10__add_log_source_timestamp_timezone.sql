ALTER TABLE log_source
    ADD COLUMN timestamp_timezone VARCHAR(80) NOT NULL DEFAULT 'UTC';

COMMENT ON COLUMN log_source.timestamp_timezone IS
    'IANA timezone used to interpret application timestamps that do not carry an offset';
