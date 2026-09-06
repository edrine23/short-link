package com.idipoedrine.shortlink.modules.analytics;

import com.idipoedrine.shortlink.modules.analytics.dto.UrlAnalyticsResponse;
import com.idipoedrine.shortlink.modules.auth.dto.AuthenticationResponse;
import com.idipoedrine.shortlink.modules.auth.dto.LoginRequest;
import com.idipoedrine.shortlink.modules.auth.dto.RegisterRequest;
import com.idipoedrine.shortlink.modules.url.dto.CreateShortUrlRequest;
import com.idipoedrine.shortlink.modules.url.dto.ShortUrlResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
@ActiveProfiles("test")
class AnalyticsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void aVisitToARedirectShowsUpInTheOwnersAnalytics() {
        HttpHeaders ownerHeaders = registerLoginAndAuthHeaders("analytics-owner@example.com");
        HttpHeaders strangerHeaders = registerLoginAndAuthHeaders("analytics-stranger@example.com");

        ResponseEntity<ShortUrlResponse> createResponse = restTemplate.exchange(
                "/api/v1/urls", HttpMethod.POST,
                new HttpEntity<>(new CreateShortUrlRequest("https://spring.io/projects/spring-boot", null), ownerHeaders),
                ShortUrlResponse.class);
        var urlId = createResponse.getBody().id();
        String shortCode = createResponse.getBody().shortCode();

        TestRestTemplate noFollow = restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW);
        noFollow.exchange("/" + shortCode, HttpMethod.GET, HttpEntity.EMPTY, Void.class);
        noFollow.exchange("/" + shortCode, HttpMethod.GET, HttpEntity.EMPTY, Void.class);

        ResponseEntity<UrlAnalyticsResponse> analyticsResponse = restTemplate.exchange(
                "/api/v1/urls/" + urlId + "/analytics", HttpMethod.GET,
                new HttpEntity<>(ownerHeaders), UrlAnalyticsResponse.class);
        assertThat(analyticsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(analyticsResponse.getBody().totalVisits()).isEqualTo(2);
        assertThat(analyticsResponse.getBody().lastVisitedAt()).isNotNull();

        ResponseEntity<String> asStranger = restTemplate.exchange(
                "/api/v1/urls/" + urlId + "/analytics", HttpMethod.GET,
                new HttpEntity<>(strangerHeaders), String.class);
        assertThat(asStranger.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpHeaders registerLoginAndAuthHeaders(String email) {
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest(email, "a-valid-password"), Void.class);
        ResponseEntity<AuthenticationResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, "a-valid-password"), AuthenticationResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginResponse.getBody().accessToken());
        return headers;
    }
}