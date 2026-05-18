package com.suburbscore.suburb.kafka;

import java.time.LocalDateTime;

public record SuburbDataUpdatedEvent(
        String eventType,        // FULL_REFRESH | TRANSPORT | CRIME | RENT | SCHOOLS | PROPERTIES
        int    suburbsUpdated,
        LocalDateTime refreshedAt
) {
    public static SuburbDataUpdatedEvent fullRefresh(int count) {
        return new SuburbDataUpdatedEvent("FULL_REFRESH", count, LocalDateTime.now());
    }
}
