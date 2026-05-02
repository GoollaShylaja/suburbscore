package com.suburbscore.user.dto;

import com.suburbscore.user.entity.LookingTo;
import com.suburbscore.user.entity.PropertyType;

import java.time.LocalDateTime;

public record PreferencesResponse(
        // Intent
        LookingTo lookingTo,

        // Budget
        Integer maxRentPerWeek,
        Integer maxPurchasePrice,

        // Property
        Integer bedroomsNeeded,
        PropertyType preferredPropertyType,
        Boolean needsParking,
        Boolean needsGarden,
        Integer bathroomsNeeded,

        // Lifestyle
        String workplaceSuburb,
        String partnerWorkplaceSuburb,
        Boolean hasChildren,
        Boolean hasPets,

        // Importance weights (1-5)
        Integer importanceCommute,
        Integer importanceSafety,
        Integer importanceSchools,
        Integer importanceWalkability,
        Integer importanceParks,

        // Notifications
        Boolean buyModeWaitlist,

        LocalDateTime updatedAt
) {}
