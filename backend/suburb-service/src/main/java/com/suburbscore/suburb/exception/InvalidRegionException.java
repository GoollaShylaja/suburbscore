package com.suburbscore.suburb.exception;

public class InvalidRegionException extends AppException {
    public InvalidRegionException(String region) {
        super("Invalid or unknown region: " + region);
    }
}
