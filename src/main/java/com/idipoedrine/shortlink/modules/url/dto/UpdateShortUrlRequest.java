package com.idipoedrine.shortlink.modules.url.dto;

import jakarta.validation.constraints.Future;

import java.time.Instant;

public record UpdateShortUrlRequest(
        Boolean active,
        @Future Instant expiresAt
) {
}
