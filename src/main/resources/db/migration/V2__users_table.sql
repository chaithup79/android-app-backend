CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(255),
    display_name VARCHAR(255),
    photo_url TEXT,
    created_at BIGINT NOT NULL,
    last_login_at BIGINT NOT NULL
);

DROP INDEX IF EXISTS idx_saved_items_user_id;

ALTER TABLE saved_items DROP COLUMN user_id;
ALTER TABLE saved_items ADD COLUMN user_id BIGINT REFERENCES users(id);

CREATE INDEX idx_saved_items_user_id ON saved_items(user_id);
