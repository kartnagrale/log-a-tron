CREATE TABLE saved_search (
 id UUID PRIMARY KEY,
 owner_user_id UUID NOT NULL REFERENCES user_account(id),
 project_id UUID NOT NULL REFERENCES project(id),
 name VARCHAR(120) NOT NULL,
 visibility VARCHAR(20) NOT NULL,
 criteria JSONB NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_saved_search_owner_name UNIQUE(owner_user_id,name),
 CONSTRAINT ck_saved_search_visibility CHECK (visibility IN ('OWNER','PROJECT'))
);
CREATE INDEX idx_saved_search_owner ON saved_search(owner_user_id,updated_at DESC);
CREATE INDEX idx_saved_search_project ON saved_search(project_id,updated_at DESC);
