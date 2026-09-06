package com.idipoedrine.shortlink.modules.url;

import com.idipoedrine.shortlink.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Single source of truth for "does this short URL belong to this owner".
 * ShortUrlService and AnalyticsService both go through this rather than
 * querying ShortUrlRepository directly, so the check can't silently diverge
 * between modules (plan doc, section 8).
 */
@Component
@RequiredArgsConstructor
public class ShortUrlOwnershipGuard {

    private final ShortUrlRepository shortUrlRepository;

    /**
     * Deliberately doesn't distinguish "doesn't exist" from "exists but
     * isn't yours" — both return 404, so ownership can't be probed by
     * trying ids that belong to someone else.
     */
    public ShortUrl requireOwned(UUID ownerId, UUID shortUrlId) {
        return shortUrlRepository.findByIdAndOwnerId(shortUrlId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
    }
}
