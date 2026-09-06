package com.idipoedrine.shortlink.modules.redirect;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.idipoedrine.shortlink.common.exception.ResourceNotFoundException;
import com.idipoedrine.shortlink.common.exception.ShortUrlInactiveException;
import com.idipoedrine.shortlink.common.exception.UrlExpiredException;
import com.idipoedrine.shortlink.config.CacheConfiguration;
import com.idipoedrine.shortlink.modules.url.ShortUrl;
import com.idipoedrine.shortlink.modules.url.ShortUrlCache;
import com.idipoedrine.shortlink.modules.url.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    private RedirectService redirectService;

    @BeforeEach
    void setUp() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CacheConfiguration.SHORT_URL_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder());
        redirectService = new RedirectService(shortUrlRepository, new ShortUrlCache(cacheManager));
    }

    @Test
    void resolvesAnActiveUnexpiredShortCodeToItsDestination() {
        when(shortUrlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(
                ShortUrl.builder().shortCode("abc1234").originalUrl("https://spring.io").active(true).build()));

        assertThat(redirectService.resolve("abc1234")).isEqualTo(URI.create("https://spring.io"));
    }

    @Test
    void onlyHitsTheDatabaseOnceAcrossRepeatedResolutions() {
        when(shortUrlRepository.findByShortCode("cached1")).thenReturn(Optional.of(
                ShortUrl.builder().shortCode("cached1").originalUrl("https://spring.io").active(true).build()));

        redirectService.resolve("cached1");
        redirectService.resolve("cached1");
        redirectService.resolve("cached1");

        verify(shortUrlRepository, times(1)).findByShortCode("cached1");
    }

    @Test
    void rejectsAnUnknownShortCode() {
        when(shortUrlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> redirectService.resolve("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsAnInactiveShortCode() {
        when(shortUrlRepository.findByShortCode("disabled")).thenReturn(Optional.of(
                ShortUrl.builder().shortCode("disabled").originalUrl("https://spring.io").active(false).build()));

        assertThatThrownBy(() -> redirectService.resolve("disabled"))
                .isInstanceOf(ShortUrlInactiveException.class);
    }

    @Test
    void rejectsAnExpiredShortCodeEvenThoughItWasCachedWhileStillValid() throws InterruptedException {
        when(shortUrlRepository.findByShortCode("expiring")).thenReturn(Optional.of(
                ShortUrl.builder().shortCode("expiring").originalUrl("https://spring.io").active(true)
                        .expiresAt(Instant.now().plus(30, ChronoUnit.MILLIS)).build()));

        redirectService.resolve("expiring"); // caches while still valid
        Thread.sleep(50);

        assertThatThrownBy(() -> redirectService.resolve("expiring"))
                .isInstanceOf(UrlExpiredException.class);
    }
}