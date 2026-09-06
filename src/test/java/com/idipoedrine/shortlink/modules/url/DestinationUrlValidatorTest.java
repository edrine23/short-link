package com.idipoedrine.shortlink.modules.url;

import com.idipoedrine.shortlink.common.exception.InvalidDestinationUrlException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DestinationUrlValidatorTest {

    private final DestinationUrlValidator validator = new DestinationUrlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "https://spring.io/projects/spring-boot",
            "http://example.com/path?query=1"
    })
    void acceptsHttpAndHttpsUrls(String url) {
        assertThatCode(() -> validator.validate(url)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file",
            "javascript:alert(1)",
            "not a url at all",
            "http://"
    })
    void rejectsUnsupportedOrMalformedUrls(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(InvalidDestinationUrlException.class);
    }
}