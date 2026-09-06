package com.idipoedrine.shortlink.modules.analytics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UrlVisitedEventListenerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private UrlVisitedEventListener listener;

    @Test
    void delegatesToAnalyticsService() {
        UrlVisitedEvent event = new UrlVisitedEvent(UUID.randomUUID(), Instant.now(), "curl/8.0", null);

        listener.onUrlVisited(event);

        verify(analyticsService).recordVisit(event);
    }

    @Test
    void swallowsAFailureFromAnalyticsServiceRatherThanPropagatingIt() {
        UrlVisitedEvent event = new UrlVisitedEvent(UUID.randomUUID(), Instant.now(), null, null);
        doThrow(new RuntimeException("db is down")).when(analyticsService).recordVisit(event);

        assertThatCode(() -> listener.onUrlVisited(event)).doesNotThrowAnyException();
    }
}