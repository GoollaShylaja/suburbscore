package com.suburbscore.suburb.controller;

import com.suburbscore.suburb.dto.*;
import com.suburbscore.suburb.service.SuburbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suburbs")
@RequiredArgsConstructor
@Tag(name = "Suburbs", description = "Sydney suburb data — demographics, transport, schools, and rental stats")
public class SuburbController {

    private final SuburbService suburbService;

    // ── GET /api/suburbs ──────────────────────────────────────────────────────

    @Operation(
        summary = "List all suburbs",
        description = "Returns a paginated list of all Sydney suburbs. Sorted by name ascending by default. " +
                      "Use ?page=0&size=20&sort=name,asc for explicit control."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of suburbs returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    public ResponseEntity<Page<SuburbSummaryResponse>> listAll(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(suburbService.findAll(pageable));
    }

    // ── GET /api/suburbs/{id} ─────────────────────────────────────────────────

    @Operation(
        summary = "Get suburb by ID",
        description = "Returns full detail for a single suburb including statistics, transport access, and school data."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suburb found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SuburbDetailResponse.class))),
        @ApiResponse(responseCode = "400", description = "ID is not a valid UUID",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {
                      "type": "about:blank",
                      "title": "Bad Request",
                      "status": 400,
                      "detail": "Invalid value 'abc' for parameter 'id'"
                    }"""))),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {
                      "type": "about:blank",
                      "title": "Not Found",
                      "status": 404,
                      "detail": "Suburb not found: 3fa85f64-5717-4562-b3fc-2c963f66afa6"
                    }""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SuburbDetailResponse> getById(
            @Parameter(description = "Suburb UUID", example = "00000000-0000-0000-0000-000000000001")
            @PathVariable UUID id) {
        return ResponseEntity.ok(suburbService.findById(id));
    }

    // ── GET /api/suburbs/postcode/{postcode} ──────────────────────────────────

    @Operation(
        summary = "Find suburbs by postcode",
        description = "Returns all suburbs matching the given 4-digit postcode. " +
                      "Multiple suburbs can share a postcode (e.g. 2065 covers St Leonards and Crows Nest)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "One or more suburbs found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SuburbDetailResponse.class))),
        @ApiResponse(responseCode = "404", description = "No suburbs found for this postcode",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {
                      "type": "about:blank",
                      "title": "Not Found",
                      "status": 404,
                      "detail": "Suburb not found: postcode 9999"
                    }""")))
    })
    @GetMapping("/postcode/{postcode}")
    public ResponseEntity<List<SuburbDetailResponse>> getByPostcode(
            @Parameter(description = "4-digit Australian postcode", example = "2042")
            @PathVariable String postcode) {
        return ResponseEntity.ok(suburbService.findByPostcode(postcode));
    }

    // ── GET /api/suburbs/{id}/stats ───────────────────────────────────────────

    @Operation(
        summary = "Get suburb statistics",
        description = "Returns the full statistical profile for a suburb: rent, crime index, walkability, " +
                      "demographics, transport access times, and school data."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stats returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SuburbStatsResponse.class))),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {
                      "type": "about:blank",
                      "title": "Not Found",
                      "status": 404,
                      "detail": "Suburb not found: 3fa85f64-5717-4562-b3fc-2c963f66afa6"
                    }""")))
    })
    @GetMapping("/{id}/stats")
    public ResponseEntity<SuburbStatsResponse> getStats(
            @Parameter(description = "Suburb UUID", example = "00000000-0000-0000-0000-000000000001")
            @PathVariable UUID id) {
        return ResponseEntity.ok(suburbService.findStatsBySuburbId(id));
    }
}
