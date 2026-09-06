CREATE TABLE url_visits (
                            id           UUID PRIMARY KEY,
                            short_url_id UUID NOT NULL REFERENCES short_urls(id),
                            visited_at   TIMESTAMP NOT NULL,
                            user_agent   VARCHAR(512),
                            referrer     VARCHAR(512)
);

-- One composite index, not two separate ones: every analytics query filters
-- by short_url_id AND ranges on visited_at together, so a composite index
-- serves that pattern far better than single-column indexes on each.
CREATE INDEX idx_url_visits_short_url_visited_at ON url_visits (short_url_id, visited_at);