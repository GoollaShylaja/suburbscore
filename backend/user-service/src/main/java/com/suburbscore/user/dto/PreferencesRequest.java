package com.suburbscore.user.dto;

import com.suburbscore.user.entity.LookingTo;
import com.suburbscore.user.entity.PropertyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "User property search preferences")
public record PreferencesRequest(
        // Intent
        @Schema(example = "RENT", description = "RENT, BUY, or BOTH")
        LookingTo lookingTo,

        // Budget
        @Schema(example = "600", description = "Maximum weekly rent in AUD (applicable when lookingTo is RENT or BOTH)")
        Integer maxRentPerWeek,

        @Schema(example = "850000", description = "Maximum purchase price in AUD (applicable when lookingTo is BUY or BOTH)")
        Integer maxPurchasePrice,

        // Property
        @Min(1) @Max(10)
        @Schema(example = "3", description = "Number of bedrooms needed (1-10)")
        Integer bedroomsNeeded,

        @Schema(example = "HOUSE", description = "APARTMENT, HOUSE, TOWNHOUSE, or UNIT")
        PropertyType preferredPropertyType,

        @Schema(example = "true")
        Boolean needsParking,

        @Schema(example = "false")
        Boolean needsGarden,

        @Min(1) @Max(5)
        @Schema(example = "2", description = "Number of bathrooms needed (1-5)")
        Integer bathroomsNeeded,

        // Lifestyle
        @Schema(example = "Sydney CBD")
        String workplaceSuburb,

        @Schema(example = "Parramatta")
        String partnerWorkplaceSuburb,

        @Schema(example = "true")
        Boolean hasChildren,

        @Schema(example = "false")
        Boolean hasPets,

        // Importance weights (1-5)
        @Min(1) @Max(5)
        @Schema(example = "5", description = "Importance of commute time (1 = not important, 5 = very important)")
        Integer importanceCommute,

        @Min(1) @Max(5)
        @Schema(example = "4", description = "Importance of safety (1-5)")
        Integer importanceSafety,

        @Min(1) @Max(5)
        @Schema(example = "5", description = "Importance of school quality (1-5)")
        Integer importanceSchools,

        @Min(1) @Max(5)
        @Schema(example = "3", description = "Importance of walkability (1-5)")
        Integer importanceWalkability,

        @Min(1) @Max(5)
        @Schema(example = "2", description = "Importance of nearby parks (1-5)")
        Integer importanceParks,

        // Notifications
        @Schema(example = "false", description = "Join waitlist for buy mode alerts")
        Boolean buyModeWaitlist
) {}
