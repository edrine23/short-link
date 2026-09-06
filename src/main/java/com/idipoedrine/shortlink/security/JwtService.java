package com.idipoedrine.shortlink.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.secret());

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.accessExpiration());
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    /**
     * Returns the token's subject if it is well-formed, signed correctly,
     * and unexpired — null otherwise. Callers treat null as "not
     * authenticated" rather than propagating the parsing exception; a bad
     * token here is an expected outcome, not a bug.
     */
    public String extractValidSubject(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}