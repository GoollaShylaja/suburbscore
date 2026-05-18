package com.suburbscore.suburb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Demographic and economic statistics for a suburb")
public record SuburbStatsResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID suburbId,
        @Schema(example = "620", description = "Median weekly rent across all property types (AUD)") Integer medianRentWeekly,
        @Schema(example = "35.5", description = "Crime index 0–100; lower is safer") BigDecimal crimeIndex,
        @Schema(example = "82.0", description = "Walk Score 0–100") BigDecimal walkabilityScore,
        @Schema(example = "47", description = "Count of walkable amenities within 1km") Integer walkabilityAmenityCount,
        @Schema(example = "8", description = "Number of parks within suburb boundary") Integer parksCount,
        @Schema(example = "45", description = "Percentage of housing stock that are houses") Integer pctHouses,
        @Schema(example = "38", description = "Percentage that are apartments") Integer pctApartments,
        @Schema(example = "10", description = "Percentage that are townhouses") Integer pctTownhouses,
        @Schema(example = "7", description = "Percentage that are units") Integer pctUnits,
        @Schema(example = "15420") Integer population,
        @Schema(example = "32") Integer medianAge,
        @Schema(example = "4.8", description = "Unemployment rate as a percentage") BigDecimal unemploymentRate,
        LocalDateTime updatedAt
) {
    public static SuburbStatsResponse empty(UUID suburbId) {
        return new SuburbStatsResponse(
                suburbId, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }
}
