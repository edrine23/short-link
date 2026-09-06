package com.idipoedrine.shortlink.modules.auth.dto;

public record AuthenticationResponse(String accessToken, String tokenType) {

    public static AuthenticationResponse bearer(String accessToken) {
        return new AuthenticationResponse(accessToken, "Bearer");
    }
}