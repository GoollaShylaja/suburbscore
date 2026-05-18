package com.suburbscore.suburb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Full suburb detail including stats, transport, schools, and rent data")
public record SuburbDetailResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
        @Schema(example = "Newtown") String name,
        @Schema(example = "2042") String postcode,
        @Schema(example = "Inner West Council") String lga,
        @Schema(example = "INNER_WEST") String region,
        @Schema(example = "-33.8979") BigDecimal latitude,
        @Schema(example = "151.1792") BigDecimal longitude,
        SuburbStatsResponse stats,
        TransportDataResponse transport,
        SchoolDataResponse schoolData,
        List<SuburbRentDTO> rentData,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
