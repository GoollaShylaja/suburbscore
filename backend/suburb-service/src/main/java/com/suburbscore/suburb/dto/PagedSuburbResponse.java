package com.suburbscore.suburb.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated list of suburb summaries")
public record PagedSuburbResponse(
        @Schema(description = "List of suburb summaries on this page") List<SuburbSummaryResponse> suburbs,
        @Schema(example = "0") Integer currentPage,
        @Schema(example = "32") Integer totalPages,
        @Schema(example = "650") Long totalElements,
        @Schema(example = "20") Integer pageSize
) {}
