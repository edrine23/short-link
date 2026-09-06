package com.idipoedrine.shortlink.modules.analytics;

import java.time.Instant;
import java.util.UUID;

public record UrlVisitedEvent(UUID shortUrlId, Instant visitedAt, String userAgent, String referrer) {
}