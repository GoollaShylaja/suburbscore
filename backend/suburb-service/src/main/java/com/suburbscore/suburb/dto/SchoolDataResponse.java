package com.suburbscore.suburb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "School availability and academic performance data for a suburb")
public record SchoolDataResponse(
        @Schema(example = "3") Integer numPrimarySchools,
        @Schema(example = "1") Integer numHighSchools,
        @Schema(example = "530.5", description = "Average NAPLAN score across schools in the suburb") BigDecimal avgNaplanScore,
        @Schema(example = "Newtown High School of the Performing Arts") String bestSchoolName,
        LocalDateTime updatedAt
) {}
