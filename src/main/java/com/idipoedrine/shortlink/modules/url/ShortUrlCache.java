package com.idipoedrine.shortlink.modules.url;

import com.idipoedrine.shortlink.config.CacheConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Owns all cache interaction for short-code resolution. RedirectService
 * reads through this; ShortUrlService evicts through this whenever a
 * mutation could change how a code resolves (plan doc, section 11.1).
 */
@Component
public class ShortUrlCache {

    private final Cache cache;

    public ShortUrlCache(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CacheConfiguration.SHORT_URL_CACHE);
    }

    public Optional<CachedShortUrlView> get(String shortCode) {
        return Optional.ofNullable(cache.get(shortCode, CachedShortUrlView.class));
    }

    public void put(String shortCode, CachedShortUrlView view) {
        cache.put(shortCode, view);
    }

    public void evict(String shortCode) {
        cache.evict(shortCode);
    }

    public static CachedShortUrlView viewOf(ShortUrl shortUrl) {
        return new CachedShortUrlView(shortUrl.getId(), shortUrl.getOriginalUrl(), shortUrl.isActive(), shortUrl.getExpiresAt());
    }
}
