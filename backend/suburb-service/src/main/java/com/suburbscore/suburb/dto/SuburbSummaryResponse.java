package com.suburbscore.suburb.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Suburb summary returned in paginated list responses")
public record SuburbSummaryResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
        @Schema(example = "Newtown") String name,
        @Schema(example = "2042") String postcode,
        @Schema(example = "Inner West Council") String lga,
        @Schema(example = "Inner West") String region,
        @Schema(example = "-33.8979") BigDecimal latitude,
        @Schema(example = "151.1792") BigDecimal longitude
) {}
