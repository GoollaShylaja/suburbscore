package com.suburbscore.user.dto;

import com.suburbscore.user.entity.LookingTo;
import com.suburbscore.user.entity.PropertyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Saved user preferences")
public record PreferencesResponse(
        @Schema(example = "RENT") LookingTo lookingTo,
        @Schema(example = "600") Integer maxRentPerWeek,
        @Schema(example = "850000") Integer maxPurchasePrice,
        @Schema(example = "3") Integer bedroomsNeeded,
        @Schema(example = "HOUSE") PropertyType preferredPropertyType,
        @Schema(example = "true") Boolean needsParking,
        @Schema(example = "false") Boolean needsGarden,
        @Schema(example = "2") Integer bathroomsNeeded,
        @Schema(example = "Sydney CBD") String workplaceSuburb,
        @Schema(example = "Parramatta") String partnerWorkplaceSuburb,
        @Schema(example = "true") Boolean hasChildren,
        @Schema(example = "false") Boolean hasPets,
        @Schema(example = "5") Integer importanceCommute,
        @Schema(example = "4") Integer importanceSafety,
        @Schema(example = "5") Integer importanceSchools,
        @Schema(example = "3") Integer importanceWalkability,
        @Schema(example = "2") Integer importanceParks,
        @Schema(example = "false") Boolean buyModeWaitlist,
        @Schema(example = "2026-05-03T10:30:00") LocalDateTime updatedAt
) {}
