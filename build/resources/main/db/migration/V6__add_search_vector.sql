ALTER TABLE saved_items ADD COLUMN search_vector tsvector;
CREATE INDEX idx_saved_items_search_vector ON saved_items USING GIN(search_vector);

CREATE OR REPLACE FUNCTION update_saved_items_search_vector() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
     setweight(to_tsvector('english', coalesce(NEW.title, '')), 'A') ||
     setweight(to_tsvector('english', coalesce(NEW.ai_summary, '')), 'A') ||
     setweight(to_tsvector('english', coalesce(NEW.summary, '')), 'B') ||
     setweight(to_tsvector('english', coalesce(NEW.category, '')), 'C') ||
     setweight(to_tsvector('english', coalesce(NEW.ai_category, '')), 'C') ||
     setweight(to_tsvector('english', coalesce(NEW.source_domain, '')), 'D');
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_saved_items_search_vector
  BEFORE INSERT OR UPDATE ON saved_items
  FOR EACH ROW EXECUTE FUNCTION update_saved_items_search_vector();
  
-- Update existing rows so they get indexed immediately
UPDATE saved_items SET version = version;
