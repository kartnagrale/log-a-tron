CREATE TABLE collector_agent (
    id UUID PRIMARY KEY,
    server_id UUID NOT NULL UNIQUE REFERENCES server_node(id) ON DELETE CASCADE,
    agent_key VARCHAR(160) NOT NULL UNIQUE,
    token_hash CHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    collector_version VARCHAR(80),
    applied_config_hash CHAR(64),
    last_seen_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_collector_agent_server ON collector_agent(server_id);
CREATE INDEX idx_collector_agent_last_seen ON collector_agent(last_seen_at);

COMMENT ON TABLE collector_agent IS
    'Managed LOG-A-TRON shipper identity for one server. The bootstrap token is stored only as a SHA-256 hash.';
