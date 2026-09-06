package com.idipoedrine.shortlink.modules.url;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RandomShortCodeGeneratorTest {

    private final RandomShortCodeGenerator generator = new RandomShortCodeGenerator();

    @Test
    void generatesCodesOfConsistentLength() {
        assertThat(generator.generate()).hasSize(7);
    }

    @Test
    void generatesDistinctCodesAcrossManyCalls() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }
        // Not proof of uniqueness — just a smoke test against accidentally
        // returning a constant or a low-entropy sequence.
        assertThat(codes).hasSize(1000);
    }
}