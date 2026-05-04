package com.suburbscore.suburb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Public transport accessibility data for a suburb")
public record TransportDataResponse(
        @Schema(example = "Newtown Station") String nearestTrainStation,
        @Schema(example = "5", description = "Walking minutes to nearest station. Null for bus-only areas.") Integer trainStationWalkMins,
        @Schema(example = "8") Integer numBusRoutes,
        @Schema(example = "18", description = "Estimated CBD commute by train/ferry/light rail in minutes. Null for bus-only areas.") Integer cbdCommuteMinsTrain,
        @Schema(example = "25") Integer cbdCommuteMinsBus,
        LocalDateTime updatedAt
) {}
