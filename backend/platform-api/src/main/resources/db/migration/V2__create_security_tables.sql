CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    username VARCHAR(120) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    email VARCHAR(320),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_issuer_subject UNIQUE (issuer, subject),
    CONSTRAINT uq_user_issuer_username UNIQUE (issuer, username)
);

CREATE TABLE role_definition (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    description VARCHAR(300) NOT NULL
);

CREATE TABLE project_membership (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account(id),
    project_id UUID NOT NULL REFERENCES project(id),
    role_id UUID NOT NULL REFERENCES role_definition(id),
    valid_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_membership_user_project_role UNIQUE (user_id, project_id, role_id)
);

CREATE TABLE environment_grant (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL REFERENCES project_membership(id) ON DELETE CASCADE,
    environment_id UUID NOT NULL REFERENCES environment(id),
    service_id UUID REFERENCES service_definition(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_environment_grant UNIQUE NULLS NOT DISTINCT (membership_id, environment_id, service_id)
);

CREATE INDEX idx_membership_user ON project_membership(user_id);
CREATE INDEX idx_membership_project ON project_membership(project_id);
CREATE INDEX idx_grant_membership ON environment_grant(membership_id);

