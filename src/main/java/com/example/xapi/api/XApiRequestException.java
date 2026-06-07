package com.example.xapi.api;

public class XApiRequestException extends RuntimeException {
    public XApiRequestException(String message) {
        super(message);
    }

    public XApiRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

