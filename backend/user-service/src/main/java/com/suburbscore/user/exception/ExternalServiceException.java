package com.suburbscore.user.exception;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String service, String reason) {
        super(service + " service unavailable: " + reason);
    }
}
