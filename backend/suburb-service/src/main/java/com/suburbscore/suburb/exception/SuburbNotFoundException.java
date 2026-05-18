package com.suburbscore.suburb.exception;

import java.util.UUID;

public class SuburbNotFoundException extends AppException {
    public SuburbNotFoundException(UUID id) {
        super("Suburb not found with id: " + id);
    }
    public SuburbNotFoundException(String message) {
        super(message);
    }
}
