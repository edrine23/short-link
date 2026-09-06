package com.idipoedrine.shortlink.modules.url;



import java.time.Instant;
import java.util.UUID;

/**
 * Cached snapshot of only what redirect resolution needs — deliberately
 * excludes the owner and created timestamp.
 */

public record CachedShortUrlView(UUID id, String originalUrl, boolean active, Instant expiresAt)
{
    public static CachedShortUrlView viewOf(ShortUrl shortUrl) {
        return new CachedShortUrlView(shortUrl.getId(), shortUrl.getOriginalUrl(), shortUrl.isActive(), shortUrl.getExpiresAt());
    }
}