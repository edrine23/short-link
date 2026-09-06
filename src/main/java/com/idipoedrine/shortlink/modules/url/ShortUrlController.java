package com.idipoedrine.shortlink.modules.url;

import com.idipoedrine.shortlink.modules.url.dto.CreateShortUrlRequest;
import com.idipoedrine.shortlink.modules.url.dto.ShortUrlResponse;
import com.idipoedrine.shortlink.modules.url.dto.UpdateShortUrlRequest;
import com.idipoedrine.shortlink.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody CreateShortUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shortUrlService.create(principal.getId(), request));
    }

    @GetMapping
    public ResponseEntity<Page<ShortUrlResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(shortUrlService.list(principal.getId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> get(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable UUID id) {
        return ResponseEntity.ok(shortUrlService.get(principal.getId(), id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody UpdateShortUrlRequest request) {
        return ResponseEntity.ok(shortUrlService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable UUID id) {
        shortUrlService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}