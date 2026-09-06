package com.idipoedrine.shortlink.modules.redirect;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.idipoedrine.shortlink.common.exception.ResourceNotFoundException;
import com.idipoedrine.shortlink.common.exception.ShortUrlInactiveException;
import com.idipoedrine.shortlink.common.exception.UrlExpiredException;
import com.idipoedrine.shortlink.config.CacheConfiguration;
import com.idipoedrine.shortlink.modules.analytics.UrlVisitedEvent;
import com.idipoedrine.shortlink.modules.url.ShortUrl;
import com.idipoedrine.shortlink.modules.url.ShortUrlCache;
import com.idipoedrine.shortlink.modules.url.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationEventPublisher;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RedirectService redirectService;

    @BeforeEach
    void setUp() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CacheConfiguration.SHORT_URL_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder());
        redirectService = new RedirectService(shortUrlRepository, new ShortUrlCache(cacheManager), eventPublisher);
    }

    @Test
    void resolvesAnActiveUnexpiredShortCodeToItsDestination() {
        when(shortUrlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(
                ShortUrl.builder().id(UUID.randomUUID()).shortCode("abc1234")
                        .originalUrl("https://spring.io").active(true).build()));

        assertThat(redirectService.resolve("abc1234", "curl/8.0", null))
                .isEqualTo(URI.create("https://spring.io"));
    }

    @Test
    void publishesAVisitEventOnASuccessfulResolution() {
        UUID shortUrlId = UUID.randomUUID();
        when(shortUrlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(
                ShortUrl.builder().id(shortUrlId).shortCode("abc1234")
                        .originalUrl("https://spring.io").active(true).build()));

        redirectService.resolve("abc1234", "curl/8.0", "https://google.com");

        ArgumentCaptor<UrlVisitedEvent> captor = ArgumentCaptor.forClass(UrlVisitedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().shortUrlId()).isEqualTo(shortUrlId);
        assertThat(captor.getValue().userAgent()).isEqualTo("curl/8.0");
        assertThat(captor.getValue().referrer()).isEqualTo("https://google.com");
    }

    @Test
    void onlyHitsTheDatabaseOnceAcrossRepeatedResolutionsButPublishesEveryTime() {
        when(shortUrlRepository.findByShortCode("cached1")).thenReturn(Optional.of(
                ShortUrl.builder().id(UUID.randomUUID()).shortCode("cached1")
                        .originalUrl("https://spring.io").active(true).build()));

        redirectService.resolve("cached1", null, null);
        redirectService.resolve("cached1", null, null);
        redirectService.resolve("cached1", null, null);

        verify(shortUrlRepository, times(1)).findByShortCode("cached1");
        verify(eventPublisher, times(3)).publishEvent(any());
    }

    @Test
    void rejectsAnUnknownShortCodeWithoutPublishingAVisit() {
        when(shortUrlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> redirectService.resolve("missing", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void rejectsAnInactiveShortCodeWithoutPublishingAVisit() {
        when(shortUrlRepository.findByShortCode("disabled")).thenReturn(Optional.of(
                ShortUrl.builder().id(UUID.randomUUID()).shortCode("disabled")
                        .originalUrl("https://spring.io").active(false).build()));

        assertThatThrownBy(() -> redirectService.resolve("disabled", null, null))
                .isInstanceOf(ShortUrlInactiveException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void rejectsAnExpiredShortCodeEvenThoughItWasCachedWhileStillValid() throws InterruptedException {
        when(shortUrlRepository.findByShortCode("expiring")).thenReturn(Optional.of(
                ShortUrl.builder().id(UUID.randomUUID()).shortCode("expiring")
                        .originalUrl("https://spring.io").active(true)
                        .expiresAt(Instant.now().plus(30, ChronoUnit.MILLIS)).build()));

        redirectService.resolve("expiring", null, null); // caches while still valid, publishes one event
        Thread.sleep(50);

        assertThatThrownBy(() -> redirectService.resolve("expiring", null, null))
                .isInstanceOf(UrlExpiredException.class);
        verify(eventPublisher, times(1)).publishEvent(any());
    }
}