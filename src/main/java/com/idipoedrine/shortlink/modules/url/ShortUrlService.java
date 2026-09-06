package com.idipoedrine.shortlink.modules.url;

import com.idipoedrine.shortlink.common.exception.ResourceNotFoundException;
import com.idipoedrine.shortlink.modules.url.dto.CreateShortUrlRequest;
import com.idipoedrine.shortlink.modules.url.dto.ShortUrlResponse;
import com.idipoedrine.shortlink.modules.url.dto.UpdateShortUrlRequest;
import com.idipoedrine.shortlink.modules.url.mapper.ShortUrlMapper;
import com.idipoedrine.shortlink.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private static final int MAX_SHORT_CODE_ATTEMPTS = 10;

    private final ShortUrlRepository shortUrlRepository;
    private final UserRepository userRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final DestinationUrlValidator destinationUrlValidator;
    private final ShortUrlMapper mapper;

    @Transactional
    public ShortUrlResponse create(UUID ownerId, CreateShortUrlRequest request) {
        // first validate the original url
        destinationUrlValidator.validate(request.originalUrl());

        ShortUrl shortUrl = ShortUrl.builder()
                .owner(userRepository.getReferenceById(ownerId)) // proxy reference: the id is already trusted from the security context, no need for a real SELECT
                .originalUrl(request.originalUrl())
                .shortCode(generateUniqueShortCode())
                .expiresAt(request.expiresAt())
                .build();

        ShortUrl saved = saveWithCollisionRetry(shortUrl);
        log.info("Created short url {} for owner {}", saved.getShortCode(), ownerId);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> list(UUID ownerId, Pageable pageable) {
        return shortUrlRepository.findAllByOwnerId(ownerId, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse get(UUID ownerId, UUID id) {
        return mapper.toResponse(findOwned(ownerId, id));
    }

    @Transactional
    public ShortUrlResponse update(UUID ownerId, UUID id, UpdateShortUrlRequest request) {
        ShortUrl shortUrl = findOwned(ownerId, id);

        if (request.active() != null) {
            shortUrl.setActive(request.active());
        }
        if (request.expiresAt() != null) {
            shortUrl.setExpiresAt(request.expiresAt());
        }

        return mapper.toResponse(shortUrlRepository.save(shortUrl));
    }

    @Transactional
    public void delete(UUID ownerId, UUID id) {
        shortUrlRepository.delete(findOwned(ownerId, id));
    }

    //===================== Private Helpers =================================================
    /**
     * Fetches a ShortUrl only if it belongs to the given owner. Deliberately
     * does not distinguish "doesn't exist" from "exists but isn't yours" —
     * both return 404, so ownership can't be probed by trying ids that
     * belong to someone else (plan doc, section 8).
     */
    private ShortUrl findOwned(UUID ownerId, UUID id) {
        return shortUrlRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
    }

    private ShortUrl saveWithCollisionRetry(ShortUrl shortUrl) {
        try {
            return shortUrlRepository.saveAndFlush(shortUrl);
        } catch (DataIntegrityViolationException ex) {
            // Another request won the race on the same generated code between
            // our existsByShortCode check and this insert. One retry with a
            // fresh code is enough — a second collision at this code space
            // size is a one-in-billions event.
            shortUrl.setShortCode(generateUniqueShortCode());
            return shortUrlRepository.saveAndFlush(shortUrl);
        }
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < MAX_SHORT_CODE_ATTEMPTS; attempt++) {
            String candidate = shortCodeGenerator.generate();
            if (!shortUrlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique short code after " + MAX_SHORT_CODE_ATTEMPTS + " attempts");
    }
}