CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE projects (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL,
    name           VARCHAR(255) NOT NULL,
    git_url        VARCHAR(500) NOT NULL,
    branch         VARCHAR(100) NOT NULL DEFAULT 'main',
    build_tool     VARCHAR(10)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    artifact_path  VARCHAR(500),
    simulations    JSONB,
    last_synced_at TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_projects_status  ON projects(status);
