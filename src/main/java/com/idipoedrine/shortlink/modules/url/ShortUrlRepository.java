package com.idipoedrine.shortlink.modules.url;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {

    Optional<ShortUrl> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<ShortUrl> findAllByOwnerId(UUID ownerId, Pageable pageable);

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);
}