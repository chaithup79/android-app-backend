CREATE TABLE saved_items (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    user_id VARCHAR(100),
    url TEXT NOT NULL,
    source VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    category VARCHAR(255),
    summary TEXT,
    image_url TEXT,
    favicon_url TEXT,
    source_domain VARCHAR(255),
    metadata_status VARCHAR(50) DEFAULT 'READY' NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT DEFAULT 1 NOT NULL,
    sync_status VARCHAR(50) DEFAULT 'SYNCED' NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE INDEX idx_saved_items_user_id ON saved_items(user_id);
