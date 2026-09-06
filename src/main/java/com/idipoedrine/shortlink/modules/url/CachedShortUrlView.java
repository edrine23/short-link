package com.idipoedrine.shortlink.modules.url;



import java.time.Instant;

/**
 * Cached snapshot of only what redirect resolution needs — deliberately
 * excludes the owner and created timestamp.
 */
public record CachedShortUrlView(String originalUrl, boolean active, Instant expiresAt) {
}
