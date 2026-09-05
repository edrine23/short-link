package com.idipoedrine.shortlink.common.error;

import com.idipoedrine.shortlink.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsResourceNotFoundExceptionTo404() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/urls/123");

        ResponseEntity<ApiError> response = handler.handleApiException(
                new ResourceNotFoundException("Short URL not found"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Short URL not found");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/urls/123");
    }

    @Test
    void mapsUnexpectedExceptionTo500WithoutLeakingInternalMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/urls");

        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new IllegalStateException("db connection pool exhausted"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    }
}