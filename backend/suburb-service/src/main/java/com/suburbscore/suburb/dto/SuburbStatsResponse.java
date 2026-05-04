package com.suburbscore.suburb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Comprehensive statistics for a suburb including rent, safety, walkability, transport, and schools")
public record SuburbStatsResponse(
        @Schema(example = "620", description = "Median weekly rent across all property types (AUD)") Integer medianRentWeekly,
        @Schema(example = "35.5", description = "Crime index 0–100; lower is safer") BigDecimal crimeIndex,
        @Schema(example = "82.0", description = "Walk Score 0–100") BigDecimal walkabilityScore,
        @Schema(example = "15420") Integer population,
        @Schema(example = "32") Integer medianAge,
        @Schema(example = "4.8", description = "Unemployment rate as a percentage") BigDecimal unemploymentRate,
        LocalDateTime statsUpdatedAt,
        TransportDataResponse transport,
        SchoolDataResponse schools
) {}
