package com.idipoedrine.shortlink.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.jwt")
@Data
public class JwtProperties{
        private String secret;
        private long accessExpiration;
        private long refreshExpiration;
}