package com.suburbscore.suburb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Median rent data for a suburb by bedroom count and property type")
public record SuburbRentDTO(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID suburbId,
        @Schema(example = "2") Integer bedrooms,
        @Schema(example = "APARTMENT") String propertyType,
        @Schema(example = "620", description = "Median weekly rent in AUD") Integer medianRentWeekly,
        LocalDateTime updatedAt
) {}
