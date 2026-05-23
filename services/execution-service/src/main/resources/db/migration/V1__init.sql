CREATE TABLE executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    simulation_class VARCHAR(255) NOT NULL,
    test_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    duration_ms BIGINT,
    result_path VARCHAR(500),
    target_host VARCHAR(255) DEFAULT 'localhost',
    target_mode VARCHAR(20) DEFAULT 'LOCAL',
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_executions_user_id ON executions (user_id);
CREATE INDEX idx_executions_project_id ON executions (project_id);
CREATE INDEX idx_executions_created_at ON executions (created_at DESC);
