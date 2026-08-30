CREATE TABLE company (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE project (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    classification VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_company_slug UNIQUE (company_id, slug)
);

CREATE TABLE environment (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES project(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(30) NOT NULL,
    sensitivity VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_environment_project_code UNIQUE (project_id, code)
);

CREATE TABLE server_node (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environment(id),
    host_id VARCHAR(160) NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    status VARCHAR(30) NOT NULL,
    labels JSONB NOT NULL DEFAULT '{}'::jsonb,
    collector_status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    collector_last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_server_environment_host UNIQUE (environment_id, host_id)
);

CREATE TABLE service_definition (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES project(id),
    service_key VARCHAR(120) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    owner_name VARCHAR(200),
    criticality VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    labels JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_service_project_key UNIQUE (project_id, service_key)
);

CREATE TABLE service_instance (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES service_definition(id),
    server_id UUID NOT NULL REFERENCES server_node(id),
    instance_key VARCHAR(180) NOT NULL,
    version VARCHAR(80),
    status VARCHAR(30) NOT NULL,
    first_seen_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_service_instance_server_key UNIQUE (server_id, instance_key)
);

CREATE TABLE log_source (
    id UUID PRIMARY KEY,
    service_instance_id UUID NOT NULL REFERENCES service_instance(id),
    path_pattern VARCHAR(1000) NOT NULL,
    log_type VARCHAR(60) NOT NULL,
    parser_profile VARCHAR(100) NOT NULL,
    multiline_rule TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_log_source_instance_path UNIQUE (service_instance_id, path_pattern)
);

CREATE INDEX idx_environment_project ON environment(project_id);
CREATE INDEX idx_server_environment ON server_node(environment_id);
CREATE INDEX idx_service_project ON service_definition(project_id);
CREATE INDEX idx_instance_service ON service_instance(service_id);
CREATE INDEX idx_instance_server ON service_instance(server_id);
CREATE INDEX idx_log_source_instance ON log_source(service_instance_id);

