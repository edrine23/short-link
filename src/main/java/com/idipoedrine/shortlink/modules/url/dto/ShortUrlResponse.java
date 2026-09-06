package com.idipoedrine.shortlink.modules.url.dto;

import java.time.Instant;
import java.util.UUID;

public record ShortUrlResponse(
        UUID id,
        String originalUrl,
        String shortCode,
        String shortUrl,
        boolean active,
        Instant createdAt,
        Instant expiresAt
) {
}
