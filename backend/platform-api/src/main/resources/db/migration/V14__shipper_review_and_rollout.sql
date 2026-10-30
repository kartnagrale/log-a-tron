-- Additive only: existing resource UUIDs, logs, searches and investigations are untouched.
ALTER TABLE log_source ADD COLUMN shipper_options JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE log_source ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE collector_agent ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE collector_agent ADD COLUMN selected_config_version BIGINT;
ALTER TABLE collector_agent ADD COLUMN deployment_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE collector_agent ADD COLUMN deployment_protocol VARCHAR(40);
ALTER TABLE collector_config_revision ADD COLUMN validation_status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE collector_config_revision ADD COLUMN validation_message TEXT;
ALTER TABLE collector_config_revision ADD COLUMN validated_at TIMESTAMPTZ;
ALTER TABLE collector_config_revision ADD COLUMN warnings JSONB NOT NULL DEFAULT '[]'::jsonb;

-- Preserve the last acknowledged legacy configuration, but never authorize a new rollout on migration.
UPDATE collector_agent a SET selected_config_version = a.applied_config_version
WHERE EXISTS (SELECT 1 FROM collector_config_revision r WHERE r.agent_id=a.id AND r.config_version=a.applied_config_version);

CREATE TABLE collector_deployment (
    id UUID PRIMARY KEY,
    agent_id UUID NOT NULL REFERENCES collector_agent(id),
    revision_id UUID NOT NULL REFERENCES collector_config_revision(id),
    previous_version BIGINT,
    status VARCHAR(30) NOT NULL,
    action VARCHAR(30) NOT NULL,
    requested_by UUID NOT NULL REFERENCES user_account(id),
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    last_error TEXT,
    row_version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_collector_deployment_pending ON collector_deployment(agent_id) WHERE status='PENDING';
CREATE INDEX idx_collector_deployment_history ON collector_deployment(agent_id,requested_at DESC);

ALTER TABLE collector_agent ADD CONSTRAINT fk_selected_collector_revision
    FOREIGN KEY (id, selected_config_version) REFERENCES collector_config_revision(agent_id, config_version);
