CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES user_account(id),
    actor_subject VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id UUID,
    project_id UUID,
    environment_id UUID,
    outcome VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    trace_id VARCHAR(64),
    client_ip VARCHAR(45),
    user_agent VARCHAR(500),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_audit_actor_time ON audit_event(actor_subject, occurred_at DESC);
CREATE INDEX idx_audit_project_time ON audit_event(project_id, occurred_at DESC);
CREATE INDEX idx_audit_action_time ON audit_event(action, occurred_at DESC);

