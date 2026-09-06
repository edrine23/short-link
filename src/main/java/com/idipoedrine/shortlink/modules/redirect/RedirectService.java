package com.idipoedrine.shortlink.modules.redirect;

import com.idipoedrine.shortlink.common.exception.ResourceNotFoundException;
import com.idipoedrine.shortlink.common.exception.ShortUrlInactiveException;
import com.idipoedrine.shortlink.common.exception.UrlExpiredException;
import com.idipoedrine.shortlink.modules.analytics.UrlVisitedEvent;
import com.idipoedrine.shortlink.modules.url.CachedShortUrlView;
import com.idipoedrine.shortlink.modules.url.ShortUrl;
import com.idipoedrine.shortlink.modules.url.ShortUrlCache;
import com.idipoedrine.shortlink.modules.url.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedirectService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlCache shortUrlCache;
    private final ApplicationEventPublisher eventPublisher;

    public URI resolve(String shortCode, String userAgent, String referrer) {
        CachedShortUrlView view = shortUrlCache.get(shortCode).orElseGet(() -> loadAndCache(shortCode));

        if (!view.active()) {
            log.info("Redirect blocked: {} is inactive", shortCode);
            throw new ShortUrlInactiveException("This link has been disabled");
        }
        if (view.expiresAt() != null && view.expiresAt().isBefore(Instant.now())) {
            log.info("Redirect blocked: {} has expired", shortCode);
            throw new UrlExpiredException("This link has expired");
        }

        eventPublisher.publishEvent(new UrlVisitedEvent(view.id(), Instant.now(), userAgent, referrer));
        return URI.create(view.originalUrl());
    }

    private CachedShortUrlView loadAndCache(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        CachedShortUrlView view = ShortUrlCache.viewOf(shortUrl);
        shortUrlCache.put(shortCode, view);
        return view;
    }
}