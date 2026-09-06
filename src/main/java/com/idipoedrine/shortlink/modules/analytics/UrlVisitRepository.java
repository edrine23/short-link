package com.idipoedrine.shortlink.modules.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UrlVisitRepository extends JpaRepository<UrlVisit, UUID> {

    long countByShortUrlId(UUID shortUrlId);

    long countByShortUrlIdAndVisitedAtAfter(UUID shortUrlId, Instant after);

    @Query("select max(v.visitedAt) from UrlVisit v where v.shortUrl.id = :shortUrlId")
    Optional<Instant> findLastVisitedAt(@Param("shortUrlId") UUID shortUrlId);
}