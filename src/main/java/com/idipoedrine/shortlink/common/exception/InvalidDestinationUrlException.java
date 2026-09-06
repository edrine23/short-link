package com.idipoedrine.shortlink.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidDestinationUrlException extends ApiException {
    public InvalidDestinationUrlException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}