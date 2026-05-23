CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATING',
    pdf_path VARCHAR(500),
    generated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_reports_execution_id ON reports (execution_id);
CREATE INDEX idx_reports_user_id ON reports (user_id);
