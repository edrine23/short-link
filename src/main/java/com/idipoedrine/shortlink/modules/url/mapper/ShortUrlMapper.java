package com.idipoedrine.shortlink.modules.url.mapper;

import com.idipoedrine.shortlink.common.properties.AppProperties;
import com.idipoedrine.shortlink.modules.url.ShortUrl;
import com.idipoedrine.shortlink.modules.url.dto.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShortUrlMapper {

    private final AppProperties appProperties;

    public ShortUrlResponse toResponse(ShortUrl shortUrl) {
        return new ShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getOriginalUrl(),
                shortUrl.getShortCode(),
                appProperties.baseUrl() + "/" + shortUrl.getShortCode(),
                shortUrl.isActive(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt()
        );
    }
}
