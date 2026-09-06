package com.idipoedrine.shortlink.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
        @NotBlank String secret,
        @Positive long accessExpiration,
        @Positive long refreshExpiration
) {
}