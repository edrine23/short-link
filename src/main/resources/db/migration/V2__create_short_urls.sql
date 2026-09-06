CREATE TABLE short_urls (
                            id           UUID PRIMARY KEY,
                            owner_id     UUID NOT NULL REFERENCES users(id),
                            original_url VARCHAR(2048) NOT NULL,
                            short_code   VARCHAR(20)  NOT NULL UNIQUE,
                            active       BOOLEAN      NOT NULL DEFAULT TRUE,
                            created_at   TIMESTAMP    NOT NULL,
                            expires_at   TIMESTAMP
);

CREATE INDEX idx_short_urls_owner_id ON short_urls (owner_id);