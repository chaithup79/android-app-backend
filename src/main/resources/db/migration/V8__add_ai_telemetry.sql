ALTER TABLE saved_items
ADD COLUMN last_ai_attempt_at BIGINT,
ADD COLUMN last_ai_error TEXT,
ADD COLUMN ai_model VARCHAR(100),
ADD COLUMN ai_generated_at BIGINT;
