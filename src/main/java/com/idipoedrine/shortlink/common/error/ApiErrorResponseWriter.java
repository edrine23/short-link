package com.idipoedrine.shortlink.common.error;

import com.idipoedrine.shortlink.common.logging.LoggingConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Writes an ApiError JSON body directly to the servlet response. Needed for
 * failures inside the Security filter chain, before the DispatcherServlet
 * exists — GlobalExceptionHandler can't catch those.
 */
@Component
@RequiredArgsConstructor
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, HttpServletRequest request,
                      HttpStatus status, String message) throws IOException {
        ApiError apiError = ApiError.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                MDC.get(LoggingConstants.CORRELATION_ID_MDC_KEY)
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(apiError));
    }
}