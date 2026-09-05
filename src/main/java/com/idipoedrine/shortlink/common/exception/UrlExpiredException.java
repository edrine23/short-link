package com.idipoedrine.shortlink.common.exception;

import org.springframework.http.HttpStatus;

public class UrlExpiredException extends ApiException {
    public UrlExpiredException(String message) {
        super(HttpStatus.GONE, message);
    }
}