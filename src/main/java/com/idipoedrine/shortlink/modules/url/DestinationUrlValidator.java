package com.idipoedrine.shortlink.modules.url;

import com.idipoedrine.shortlink.common.exception.InvalidDestinationUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@Component
public class DestinationUrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    public void validate(String originalUrl) {
        URI uri;
        try {
            uri = new URI(originalUrl);
        } catch (URISyntaxException ex) {
            throw new InvalidDestinationUrlException("Destination URL is malformed");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new InvalidDestinationUrlException("Destination URL must use http or https");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidDestinationUrlException("Destination URL must include a host");
        }
    }
}