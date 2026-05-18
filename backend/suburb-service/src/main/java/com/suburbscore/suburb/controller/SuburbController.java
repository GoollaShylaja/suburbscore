package com.suburbscore.suburb.controller;

import com.suburbscore.suburb.dto.*;
import com.suburbscore.suburb.enums.PropertyType;
import com.suburbscore.suburb.service.SchoolDataLoaderService;
import com.suburbscore.suburb.service.SuburbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/suburbs")
@RequiredArgsConstructor
@Tag(name = "Suburb Service", description = "Sydney suburb data endpoints — demographics, transport, schools, and rental stats")
public class SuburbController {

    private final SuburbService suburbService;
    private final SchoolDataLoaderService schoolDataLoaderService;

    // ── GET /api/suburbs ──────────────────────────────────────────────────────

    @Operation(summary = "Get all Sydney suburbs paginated",
               description = "Returns a paginated list of all Sydney suburbs, sorted by name by default.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of suburbs returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PagedSuburbResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedSuburbResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort) {
        return ResponseEntity.ok(suburbService.getAllSuburbs(page, size, sort));
    }

    // ── GET /api/suburbs/{id} ─────────────────────────────────────────────────

    @Operation(summary = "Get suburb by ID",
               description = "Returns full detail including stats, transport, schools, and rent data.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suburb found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SuburbDetailResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid UUID format",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {"type":"about:blank","title":"Not Found","status":404,
                     "detail":"Suburb not found: 3fa85f64-5717-4562-b3fc-2c963f66afa6"}""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SuburbDetailResponse> getById(
            @Parameter(description = "Suburb UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(suburbService.findById(id));
    }

    // ── GET /api/suburbs/region/{region} ──────────────────────────────────────

    @Operation(summary = "Get suburbs by region",
               description = "Returns all suburbs in a Sydney region (e.g. INNER_WEST, NORTH_SHORE).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suburbs returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/region/{region}")
    public ResponseEntity<List<SuburbSummaryResponse>> getByRegion(
            @Parameter(description = "Region name", example = "INNER_WEST") @PathVariable String region) {
        return ResponseEntity.ok(suburbService.getSuburbsByRegion(region));
    }

    // ── GET /api/suburbs/postcode/{postcode} ──────────────────────────────────

    @Operation(summary = "Find suburbs by postcode")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suburbs found"),
        @ApiResponse(responseCode = "404", description = "No suburbs for this postcode",
            content = @Content(mediaType = "application/problem+json"))
    })
    @GetMapping("/postcode/{postcode}")
    public ResponseEntity<List<SuburbDetailResponse>> getByPostcode(
            @Parameter(description = "4-digit postcode", example = "2042") @PathVariable String postcode) {
        return ResponseEntity.ok(suburbService.findByPostcode(postcode));
    }

    // ── GET /api/suburbs/search ───────────────────────────────────────────────

    @Operation(summary = "Search suburbs by name", description = "Case-insensitive partial name search.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results returned"),
        @ApiResponse(responseCode = "400", description = "Name parameter is blank",
            content = @Content(mediaType = "application/problem+json"))
    })
    @GetMapping("/search")
    public ResponseEntity<List<SuburbSummaryResponse>> search(
            @Parameter(description = "Suburb name (partial match)", example = "newt")
            @RequestParam String name) {
        return ResponseEntity.ok(suburbService.searchByName(name));
    }

    // ── GET /api/suburbs/{id}/stats ───────────────────────────────────────────

    @Operation(summary = "Get suburb statistics",
               description = "Returns demographic and economic stats. Returns empty DTO (not 404) if not yet loaded.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stats returned"),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json"))
    })
    @GetMapping("/{id}/stats")
    public ResponseEntity<SuburbStatsResponse> getStats(
            @Parameter(description = "Suburb UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(suburbService.getSuburbStats(id));
    }

    // ── GET /api/suburbs/{id}/transport ───────────────────────────────────────

    @Operation(summary = "Get transport data for a suburb",
               description = "Returns empty DTO (not 404) if transport data not yet loaded.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transport data returned"),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json"))
    })
    @GetMapping("/{id}/transport")
    public ResponseEntity<TransportDataResponse> getTransport(
            @Parameter(description = "Suburb UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(suburbService.getTransportData(id));
    }

    // ── GET /api/suburbs/{id}/schools ─────────────────────────────────────────

    @Operation(summary = "Get school data for a suburb",
               description = "Returns dataAvailable=false DTO (not 404) if school data not yet loaded.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "School data returned"),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json"))
    })
    @GetMapping("/{id}/schools")
    public ResponseEntity<SchoolDataResponse> getSchools(
            @Parameter(description = "Suburb UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(suburbService.getSchoolData(id));
    }

    // ── GET /api/suburbs/{id}/rent ────────────────────────────────────────────

    @Operation(summary = "Get rent data for a suburb",
               description = "Returns all rent records or filtered by bedrooms and property type.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rent data returned"),
        @ApiResponse(responseCode = "404", description = "Suburb or specific rent record not found",
            content = @Content(mediaType = "application/problem+json"))
    })
    @GetMapping("/{id}/rent")
    public ResponseEntity<?> getRent(
            @Parameter(description = "Suburb UUID") @PathVariable UUID id,
            @Parameter(description = "Number of bedrooms") @RequestParam(required = false) Integer bedrooms,
            @Parameter(description = "Property type") @RequestParam(required = false) String type) {

        if (bedrooms != null && type != null) {
            PropertyType propertyType = PropertyType.valueOf(type.toUpperCase());
            Optional<SuburbRentDTO> result = suburbService.getRentByBedroomsAndType(id, bedrooms, propertyType);
            return result.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.ok(suburbService.getRentData(id));
    }

    // ── POST /api/suburbs/bulk-details ────────────────────────────────────────

    @Operation(summary = "Get bulk suburb details",
               description = "Returns full details for a list of suburb IDs. Used by scoring-service.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Details returned")
    })
    @PostMapping("/bulk-details")
    public ResponseEntity<List<SuburbDetailResponse>> getBulkDetails(
            @RequestBody List<UUID> suburbIds) {
        return ResponseEntity.ok(suburbService.getBulkSuburbDetails(suburbIds));
    }

    // ── PUT /api/suburbs/{id}/stats ───────────────────────────────────────────

    @Operation(summary = "Update suburb statistics", description = "Called by data-ingestion-service. Upserts stats.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stats updated"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json"))
    })
    @PutMapping("/{id}/stats")
    public ResponseEntity<SuburbStatsResponse> updateStats(
            @Parameter(description = "Suburb UUID") @PathVariable UUID id,
            @RequestBody SuburbStatsResponse dto) {
        return ResponseEntity.ok(suburbService.updateStats(id, dto));
    }

    // ── PUT /api/suburbs/{id}/transport ───────────────────────────────────────

    @Operation(summary = "Update transport data", description = "Called by data-ingestion-service.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transport data updated"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json"))
    })
    @PutMapping("/{id}/transport")
    public ResponseEntity<TransportDataResponse> updateTransport(
            @Parameter(description = "Suburb UUID") @PathVariable UUID id,
            @RequestBody TransportDataResponse dto) {
        return ResponseEntity.ok(suburbService.updateTransportData(id, dto));
    }

    // ── PUT /api/suburbs/{id}/schools ─────────────────────────────────────────

    @Operation(summary = "Update school data", description = "Called by data-ingestion-service.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "School data updated"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json"))
    })
    @PutMapping("/{id}/schools")
    public ResponseEntity<SchoolDataResponse> updateSchools(
            @Parameter(description = "Suburb UUID") @PathVariable UUID id,
            @RequestBody SchoolDataResponse dto) {
        return ResponseEntity.ok(suburbService.updateSchoolData(id, dto));
    }

    // ── PUT /api/suburbs/{id}/rent ────────────────────────────────────────────

    @Operation(summary = "Upsert rent data", description = "Called by data-ingestion-service.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rent data upserted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json"))
    })
    @PutMapping("/{id}/rent")
    public ResponseEntity<SuburbRentDTO> upsertRent(
            @Parameter(description = "Suburb UUID") @PathVariable UUID id,
            @RequestBody SuburbRentDTO dto) {
        return ResponseEntity.ok(suburbService.upsertRentData(id, dto));
    }

    // ── Saved Suburbs ─────────────────────────────────────────────────────────

    @Operation(summary = "Save a suburb for the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suburb saved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "404", description = "Suburb not found",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "409", description = "Suburb already saved",
            content = @Content(mediaType = "application/problem+json"))
    })
    @PostMapping("/saved/{suburbId}")
    public ResponseEntity<SuburbDetailResponse> saveSuburb(
            @Parameter(description = "User UUID from JWT") @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Suburb UUID") @PathVariable UUID suburbId) {
        return ResponseEntity.ok(suburbService.saveSuburb(userId, suburbId));
    }

    @Operation(summary = "Remove a saved suburb")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Suburb removed from saved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "404", description = "Saved suburb not found",
            content = @Content(mediaType = "application/problem+json"))
    })
    @DeleteMapping("/saved/{suburbId}")
    public ResponseEntity<Void> unsaveSuburb(
            @Parameter(description = "User UUID from JWT") @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Suburb UUID") @PathVariable UUID suburbId) {
        suburbService.unsaveSuburb(userId, suburbId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all saved suburbs for the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Saved suburbs returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/problem+json"))
    })
    @GetMapping("/saved")
    public ResponseEntity<List<SuburbDetailResponse>> getSavedSuburbs(
            @Parameter(description = "User UUID from JWT") @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(suburbService.getSavedSuburbs(userId));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Trigger school data reload from NSW API",
               description = "Runs asynchronously. Returns immediately.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "School data loading started"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/problem+json"))
    })
    @PostMapping("/admin/load-schools")
    public ResponseEntity<String> triggerSchoolLoad() {
        schoolDataLoaderService.reloadAllAsync();
        log.info("Admin triggered school data reload");
        return ResponseEntity.ok("School data loading started");
    }

    @Operation(summary = "Import suburbs for a specific postcode from the Postcode API",
               description = "Fetches suburb data for the given postcode from the external Postcode API and persists any new entries. Skips duplicates. Useful for adding postcodes not covered by the CSV seed.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Import complete — returns count of newly added suburbs"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/problem+json"))
    })
    @PostMapping("/admin/import/{postcode}")
    public ResponseEntity<String> importPostcode(
            @Parameter(description = "Australian postcode to import", example = "2250")
            @PathVariable int postcode) {
        int added = suburbService.importPostcode(postcode);
        return ResponseEntity.ok("Imported " + added + " new suburb(s) for postcode " + postcode);
    }
}
