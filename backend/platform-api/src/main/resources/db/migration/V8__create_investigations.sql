CREATE TABLE investigation (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES user_account(id),
    project_id UUID NOT NULL REFERENCES project(id),
    environment_id UUID REFERENCES environment(id),
    created_at TIMESTAMPTZ NOT NULL,
    from_time TIMESTAMPTZ NOT NULL,
    to_time TIMESTAMPTZ NOT NULL,
    query_kind VARCHAR(40) NOT NULL,
    query_digest VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    evidence_references JSONB NOT NULL DEFAULT '[]'::jsonb,
    quality_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ai_provider VARCHAR(80),
    ai_model VARCHAR(120),
    ai_outcome VARCHAR(30),
    ai_latency_ms BIGINT
);
CREATE INDEX ix_investigation_owner_created ON investigation(owner_user_id, created_at DESC);
CREATE INDEX ix_investigation_scope_time ON investigation(project_id, environment_id, from_time, to_time);

