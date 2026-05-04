package com.suburbscore.suburb.exception;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found: " + identifier);
    }
}
