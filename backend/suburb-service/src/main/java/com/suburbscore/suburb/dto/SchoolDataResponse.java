package com.suburbscore.suburb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "School availability and ICSEA score data for a suburb")
public record SchoolDataResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID suburbId,
        @Schema(example = "3") Integer numPrimarySchools,
        @Schema(example = "1") Integer numHighSchools,
        @Schema(example = "1087.50", description = "Average ICSEA score across schools in the suburb") BigDecimal avgIcseaScore,
        @Schema(example = "Newtown High School of the Performing Arts") String bestSchoolName,
        @Schema(example = "true") Boolean dataAvailable,
        LocalDateTime updatedAt
) {
    public static SchoolDataResponse notYetAvailable(UUID suburbId) {
        return new SchoolDataResponse(suburbId, 0, 0, null, null, false, null);
    }
}
