CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority INT DEFAULT 0,
    retry_count INT DEFAULT 0,
    payload VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX idx_jobs_status_type ON jobs(status, type);
