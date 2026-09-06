package com.idipoedrine.shortlink.modules.analytics;

import com.idipoedrine.shortlink.common.exception.ResourceNotFoundException;
import com.idipoedrine.shortlink.modules.analytics.dto.UrlAnalyticsResponse;
import com.idipoedrine.shortlink.modules.url.ShortUrl;
import com.idipoedrine.shortlink.modules.url.ShortUrlOwnershipGuard;
import com.idipoedrine.shortlink.modules.url.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private UrlVisitRepository urlVisitRepository;
    @Mock
    private ShortUrlRepository shortUrlRepository;
    @Mock
    private ShortUrlOwnershipGuard ownershipGuard;

    @InjectMocks
    private AnalyticsService analyticsService;

    private final UUID shortUrlId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @Test
    void recordsAVisitLinkedToTheShortUrlFromTheEvent() {
        ShortUrl reference = ShortUrl.builder().id(shortUrlId).build();
        when(shortUrlRepository.getReferenceById(shortUrlId)).thenReturn(reference);
        Instant visitedAt = Instant.now();

        analyticsService.recordVisit(new UrlVisitedEvent(shortUrlId, visitedAt, "curl/8.0", "https://google.com"));

        ArgumentCaptor<UrlVisit> captor = ArgumentCaptor.forClass(UrlVisit.class);
        verify(urlVisitRepository).save(captor.capture());
        assertThat(captor.getValue().getShortUrl()).isEqualTo(reference);
        assertThat(captor.getValue().getVisitedAt()).isEqualTo(visitedAt);
        assertThat(captor.getValue().getUserAgent()).isEqualTo("curl/8.0");
    }

    @Test
    void truncatesAnOverlongUserAgentRatherThanFailingTheInsert() {
        when(shortUrlRepository.getReferenceById(shortUrlId)).thenReturn(ShortUrl.builder().id(shortUrlId).build());
        String hugeUserAgent = "x".repeat(1000);

        analyticsService.recordVisit(new UrlVisitedEvent(shortUrlId, Instant.now(), hugeUserAgent, null));

        ArgumentCaptor<UrlVisit> captor = ArgumentCaptor.forClass(UrlVisit.class);
        verify(urlVisitRepository).save(captor.capture());
        assertThat(captor.getValue().getUserAgent()).hasSize(512);
    }

    @Test
    void rejectsAnalyticsRequestForAShortUrlTheCallerDoesNotOwn() {
        when(ownershipGuard.requireOwned(ownerId, shortUrlId))
                .thenThrow(new ResourceNotFoundException("Short URL not found"));

        assertThatThrownBy(() -> analyticsService.getAnalytics(ownerId, shortUrlId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returnsAggregatedStatsForAnOwnedShortUrl() {
        when(ownershipGuard.requireOwned(ownerId, shortUrlId)).thenReturn(ShortUrl.builder().id(shortUrlId).build());
        when(urlVisitRepository.countByShortUrlId(shortUrlId)).thenReturn(42L);
        when(urlVisitRepository.countByShortUrlIdAndVisitedAtAfter(any(), any())).thenReturn(5L, 20L);
        Instant lastVisit = Instant.now();
        when(urlVisitRepository.findLastVisitedAt(shortUrlId)).thenReturn(Optional.of(lastVisit));

        UrlAnalyticsResponse response = analyticsService.getAnalytics(ownerId, shortUrlId);

        assertThat(response.totalVisits()).isEqualTo(42L);
        assertThat(response.visitsLast24Hours()).isEqualTo(5L);
        assertThat(response.visitsLast7Days()).isEqualTo(20L);
        assertThat(response.lastVisitedAt()).isEqualTo(lastVisit);
    }
}