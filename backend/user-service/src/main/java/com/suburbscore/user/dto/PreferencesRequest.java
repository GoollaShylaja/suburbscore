package com.suburbscore.user.dto;

import com.suburbscore.user.entity.LookingTo;
import com.suburbscore.user.entity.PropertyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PreferencesRequest(
        // Intent
        LookingTo lookingTo,

        // Budget
        Integer maxRentPerWeek,
        Integer maxPurchasePrice,

        // Property
        @Min(1) @Max(10) Integer bedroomsNeeded,
        PropertyType preferredPropertyType,
        Boolean needsParking,
        Boolean needsGarden,
        @Min(1) @Max(5) Integer bathroomsNeeded,

        // Lifestyle
        String workplaceSuburb,
        String partnerWorkplaceSuburb,
        Boolean hasChildren,
        Boolean hasPets,

        // Importance weights (1-5)
        @Min(1) @Max(5) Integer importanceCommute,
        @Min(1) @Max(5) Integer importanceSafety,
        @Min(1) @Max(5) Integer importanceSchools,
        @Min(1) @Max(5) Integer importanceWalkability,
        @Min(1) @Max(5) Integer importanceParks,

        // Notifications
        Boolean buyModeWaitlist
) {}
