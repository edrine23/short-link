package com.idipoedrine.shortlink.modules.url.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreateShortUrlRequest(
        @NotBlank String originalUrl,
        @Future Instant expiresAt
) {
}
