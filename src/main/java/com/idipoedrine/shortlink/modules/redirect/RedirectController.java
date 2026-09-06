package com.idipoedrine.shortlink.modules.redirect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        URI destination = redirectService.resolve(
                shortCode,
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(HttpHeaders.REFERER));
        return ResponseEntity.status(HttpStatus.FOUND).location(destination).build();
    }
}