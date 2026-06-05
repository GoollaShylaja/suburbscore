package com.suburbscore.suburb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Public transport accessibility data for a suburb")
public record TransportDataResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID suburbId,
        @Schema(example = "Newtown Station") String nearestTrainStation,
        @Schema(example = "5", description = "Walking minutes to nearest rail station") Integer trainStationWalkMins,
        @Schema(example = "8", description = "Number of distinct bus routes serving the suburb") Integer numBusRoutes,
        @Schema(example = "false", description = "Whether the suburb has a ferry wharf within 1.5km") Boolean hasFerryAccess,
        @Schema(example = "18", description = "Fastest CBD commute by rail in minutes (Sydney Town Hall)") Integer cbdCommuteMinsTrain,
        @Schema(example = "35", description = "Fastest CBD commute by bus in minutes") Integer cbdCommuteMinsBus,
        LocalDateTime updatedAt
) {
    public static TransportDataResponse empty(UUID suburbId) {
        return new TransportDataResponse(suburbId, null, null, null, null, null, null, null);
    }
}
