package com.idipoedrine.shortlink.modules.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlVisitedEventListener {

    private final AnalyticsService analyticsService;

    @EventListener
    public void onUrlVisited(UrlVisitedEvent event) {
        // Recording a visit is a side effect, not the redirect's core job.
        // Publishing is synchronous and inline (no @Async yet — plan doc,
        // section 11.2), so an uncaught exception here would propagate
        // straight back through RedirectService.resolve() and turn a
        // successful redirect into a 500. Log and move on instead.
        try {
            analyticsService.recordVisit(event);
        } catch (Exception ex) {
            log.error("Failed to record visit for short url {}", event.shortUrlId(), ex);
        }
    }
}