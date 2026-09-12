ALTER TABLE collector_agent
    ALTER COLUMN token_hash TYPE VARCHAR(64),
    ALTER COLUMN applied_config_hash TYPE VARCHAR(64);

COMMENT ON COLUMN collector_agent.token_hash IS
    'SHA-256 hex digest of the one-time bootstrap token.';

COMMENT ON COLUMN collector_agent.applied_config_hash IS
    'SHA-256 hex digest of the collector configuration currently applied by the managed shipper.';
