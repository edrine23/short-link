package com.idipoedrine.shortlink.modules.redirect;

import com.idipoedrine.shortlink.modules.auth.dto.AuthenticationResponse;
import com.idipoedrine.shortlink.modules.auth.dto.LoginRequest;
import com.idipoedrine.shortlink.modules.auth.dto.RegisterRequest;
import com.idipoedrine.shortlink.modules.url.dto.CreateShortUrlRequest;
import com.idipoedrine.shortlink.modules.url.dto.ShortUrlResponse;
import com.idipoedrine.shortlink.modules.url.dto.UpdateShortUrlRequest;
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

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
@ActiveProfiles("test")
class RedirectIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void redirectsPubliclyAndStopsAfterTheOwnerDisablesTheLink() {
        HttpHeaders authHeaders = registerLoginAndAuthHeaders("redirect-test@example.com");

        ResponseEntity<ShortUrlResponse> createResponse = restTemplate.exchange(
                "/api/v1/urls", HttpMethod.POST,
                new HttpEntity<>(new CreateShortUrlRequest("https://spring.io/projects/spring-boot", null), authHeaders),
                ShortUrlResponse.class);
        String shortCode = createResponse.getBody().shortCode();

        TestRestTemplate noFollow = restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW);

        // No Authorization header at all — proves the redirect is genuinely public.
        ResponseEntity<Void> redirectResponse = noFollow.exchange(
                "/" + shortCode, HttpMethod.GET, HttpEntity.EMPTY, Void.class);
        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(redirectResponse.getHeaders().getLocation())
                .isEqualTo(URI.create("https://spring.io/projects/spring-boot"));

        // Resolve again to make sure it's actually sitting in the cache...
        noFollow.exchange("/" + shortCode, HttpMethod.GET, HttpEntity.EMPTY, Void.class);

        restTemplate.exchange("/api/v1/urls/" + createResponse.getBody().id(), HttpMethod.PATCH,
                new HttpEntity<>(new UpdateShortUrlRequest(false, null), authHeaders), Void.class);

        // ...then confirm the cache entry was actually evicted, not just the row updated.
        ResponseEntity<String> afterDisable = noFollow.exchange(
                "/" + shortCode, HttpMethod.GET, HttpEntity.EMPTY, String.class);
        assertThat(afterDisable.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(afterDisable.getBody()).contains("disabled");
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