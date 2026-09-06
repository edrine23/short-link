package com.idipoedrine.shortlink.modules.analytics;

import com.idipoedrine.shortlink.modules.analytics.dto.UrlAnalyticsResponse;
import com.idipoedrine.shortlink.modules.url.ShortUrlOwnershipGuard;
import com.idipoedrine.shortlink.modules.url.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int MAX_HEADER_LENGTH = 512;

    private final UrlVisitRepository urlVisitRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlOwnershipGuard ownershipGuard;

    @Transactional
    public void recordVisit(UrlVisitedEvent event) {
        UrlVisit visit = UrlVisit.builder()
                .shortUrl(shortUrlRepository.getReferenceById(event.shortUrlId())) // trusted, already-validated id — no need for a real SELECT
                .visitedAt(event.visitedAt())
                .userAgent(truncate(event.userAgent()))
                .referrer(truncate(event.referrer()))
                .build();
        urlVisitRepository.save(visit);
    }

    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getAnalytics(UUID ownerId, UUID shortUrlId) {
        ownershipGuard.requireOwned(ownerId, shortUrlId); // throws 404 if not owned

        Instant now = Instant.now();
        long total = urlVisitRepository.countByShortUrlId(shortUrlId);
        long last24h = urlVisitRepository.countByShortUrlIdAndVisitedAtAfter(shortUrlId, now.minus(Duration.ofHours(24)));
        long last7d = urlVisitRepository.countByShortUrlIdAndVisitedAtAfter(shortUrlId, now.minus(Duration.ofDays(7)));
        Instant lastVisitedAt = urlVisitRepository.findLastVisitedAt(shortUrlId).orElse(null);

        return new UrlAnalyticsResponse(shortUrlId, total, last24h, last7d, lastVisitedAt);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_HEADER_LENGTH ? value.substring(0, MAX_HEADER_LENGTH) : value;
    }
}