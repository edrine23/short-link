package com.idipoedrine.shortlink.modules.url;

import com.idipoedrine.shortlink.modules.auth.dto.AuthenticationResponse;
import com.idipoedrine.shortlink.modules.auth.dto.LoginRequest;
import com.idipoedrine.shortlink.modules.auth.dto.RegisterRequest;
import com.idipoedrine.shortlink.modules.url.dto.CreateShortUrlRequest;
import com.idipoedrine.shortlink.modules.url.dto.ShortUrlResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
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
@Testcontainers
@ActiveProfiles("test")
class ShortUrlOwnershipIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void aUserCannotAccessAnotherUsersShortUrl() {
        String tokenA = registerAndLogin("owner-a@example.com");
        String tokenB = registerAndLogin("owner-b@example.com");

        CreateShortUrlRequest createRequest =
                new CreateShortUrlRequest("https://spring.io/projects/spring-boot", null);
        ResponseEntity<ShortUrlResponse> createResponse = restTemplate.exchange(
                "/api/v1/urls", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeader(tokenA)), ShortUrlResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var urlId = createResponse.getBody().id();

        ResponseEntity<String> asOwner = restTemplate.exchange(
                "/api/v1/urls/" + urlId, HttpMethod.GET,
                new HttpEntity<>(authHeader(tokenA)), String.class);
        assertThat(asOwner.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> asOtherUser = restTemplate.exchange(
                "/api/v1/urls/" + urlId, HttpMethod.GET,
                new HttpEntity<>(authHeader(tokenB)), String.class);
        assertThat(asOtherUser.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest(email, "a-valid-password"), Void.class);

        ResponseEntity<AuthenticationResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, "a-valid-password"),
                AuthenticationResponse.class);
        return loginResponse.getBody().accessToken();
    }

    private HttpHeaders authHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
