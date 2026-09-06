package com.idipoedrine.shortlink.security;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;



class JwtServiceTest {

    private final JwtProperties properties;
    private final JwtService jwtService;

    JwtServiceTest() {
        properties = new JwtProperties();
        properties.setSecret(
                "c2hvcnRsaW5rLXRlc3Qtb25seS1zaWduaW5nLWtleS1kby1ub3QtdXNlLWluLXByb2Q="
        );
        properties.setAccessExpiration(900_000L);       // 15 minutes
        properties.setRefreshExpiration(604_800_000L); // 7 days

        jwtService = new JwtService(properties);
    }

    @Test
    void extractsSubjectFromAFreshlyGeneratedToken() {
        String token = jwtService.generateAccessToken("ada@example.com");

        assertThat(jwtService.extractValidSubject(token))
                .isEqualTo("ada@example.com");
    }

    @Test
    void rejectsATokenSignedWithADifferentKey() {
        JwtProperties otherKeyProperties = new JwtProperties();

        otherKeyProperties.setSecret(
                "YS1jb21wbGV0ZWx5LWRpZmZlcmVudC1zaWduaW5nLWtleS1mb3ItdGVzdGluZy1vbmx5"
        );
        otherKeyProperties.setAccessExpiration(900_000L);
        otherKeyProperties.setRefreshExpiration(604_800_000L);

        String tokenFromAnotherKey =
                new JwtService(otherKeyProperties)
                        .generateAccessToken("ada@example.com");

        assertThat(jwtService.extractValidSubject(tokenFromAnotherKey))
                .isNull();
    }

    @Test
    void rejectsAnExpiredToken() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties();

        shortLived.setSecret(properties.getSecret());
        shortLived.setAccessExpiration(1L);
        shortLived.setRefreshExpiration(properties.getRefreshExpiration());

        String token =
                new JwtService(shortLived)
                        .generateAccessToken("ada@example.com");

        Thread.sleep(5);

        assertThat(jwtService.extractValidSubject(token))
                .isNull();
    }
}