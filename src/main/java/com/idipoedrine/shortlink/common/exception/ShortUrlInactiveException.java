package com.idipoedrine.shortlink.common.exception;

import org.springframework.http.HttpStatus;

public class ShortUrlInactiveException extends ApiException {
    public ShortUrlInactiveException(String message) {
        super(HttpStatus.GONE, message);
    }
}