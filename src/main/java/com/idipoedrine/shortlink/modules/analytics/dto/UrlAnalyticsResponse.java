package com.idipoedrine.shortlink.modules.analytics.dto;

import java.time.Instant;
import java.util.UUID;

public record UrlAnalyticsResponse(
        UUID shortUrlId,
        long totalVisits,
        long visitsLast24Hours,
        long visitsLast7Days,
        Instant lastVisitedAt
) {
}