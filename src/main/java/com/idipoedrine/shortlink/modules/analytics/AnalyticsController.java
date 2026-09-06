package com.idipoedrine.shortlink.modules.analytics;

import com.idipoedrine.shortlink.modules.analytics.dto.UrlAnalyticsResponse;
import com.idipoedrine.shortlink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/urls/{id}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<UrlAnalyticsResponse> get(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable UUID id) {
        return ResponseEntity.ok(analyticsService.getAnalytics(principal.getId(), id));
    }
}