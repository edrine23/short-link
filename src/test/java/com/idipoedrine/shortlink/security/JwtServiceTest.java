package com.idipoedrine.shortlink.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtProperties properties = new JwtProperties(
            "c2hvcnRsaW5rLXRlc3Qtb25seS1zaWduaW5nLWtleS1kby1ub3QtdXNlLWluLXByb2Q=",
            900_000L,    // 15 minutes
            604_800_000L // 7 days — unused until refresh tokens are actually in scope
    );

    private final JwtService jwtService = new JwtService(properties);

    @Test
    void extractsSubjectFromAFreshlyGeneratedToken() {
        String token = jwtService.generateAccessToken("ada@example.com");

        assertThat(jwtService.extractValidSubject(token)).isEqualTo("ada@example.com");
    }

    @Test
    void rejectsATokenSignedWithADifferentKey() {
        JwtProperties otherKeyProperties = new JwtProperties(
                "YS1jb21wbGV0ZWx5LWRpZmZlcmVudC1zaWduaW5nLWtleS1mb3ItdGVzdGluZy1vbmx5",
                900_000L,
                604_800_000L
        );
        String tokenFromAnotherKey = new JwtService(otherKeyProperties).generateAccessToken("ada@example.com");

        assertThat(jwtService.extractValidSubject(tokenFromAnotherKey)).isNull();
    }

    @Test
    void rejectsAnExpiredToken() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties(properties.secret(), 1L, properties.refreshExpiration());
        String token = new JwtService(shortLived).generateAccessToken("ada@example.com");

        Thread.sleep(5); // let the 1ms expiration actually elapse

        assertThat(jwtService.extractValidSubject(token)).isNull();
    }
}