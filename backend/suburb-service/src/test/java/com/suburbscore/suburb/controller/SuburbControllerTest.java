package com.suburbscore.suburb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbscore.suburb.config.SecurityConfig;
import com.suburbscore.suburb.dto.*;
import com.suburbscore.suburb.exception.GlobalExceptionHandler;
import com.suburbscore.suburb.exception.ResourceNotFoundException;
import com.suburbscore.suburb.security.JwtAuthenticationFilter;
import com.suburbscore.suburb.service.SchoolDataLoaderService;
import com.suburbscore.suburb.service.SuburbService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = SuburbController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
    }
)
@Import({SuburbControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
@org.springframework.test.context.TestPropertySource(properties = "spring.cache.type=none")
@DisplayName("SuburbController")
class SuburbControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean SuburbService suburbService;
    @MockitoBean SchoolDataLoaderService schoolDataLoaderService;

    private static final UUID SUBURB_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private SuburbSummaryResponse buildSummary() {
        return new SuburbSummaryResponse(
                SUBURB_ID, "Newtown", "2042", "Inner West Council", "INNER_WEST",
                new BigDecimal("-33.897900"), new BigDecimal("151.179200"));
    }

    private TransportDataResponse buildTransport() {
        return new TransportDataResponse(
                SUBURB_ID, "Newtown Station", 5, 8, 18, 25,
                LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private SchoolDataResponse buildSchools() {
        return new SchoolDataResponse(
                SUBURB_ID, 2, 1, new BigDecimal("1087.50"),
                "Newtown High School of the Performing Arts",
                true, LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private SuburbStatsResponse buildStats() {
        return new SuburbStatsResponse(
                SUBURB_ID, 650, new BigDecimal("45.0"), new BigDecimal("88.0"),
                47, 8, 45, 38, 10, 7, 15420, 29, new BigDecimal("5.80"),
                LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private SuburbDetailResponse buildDetail() {
        return new SuburbDetailResponse(
                SUBURB_ID, "Newtown", "2042", "Inner West Council", "INNER_WEST",
                new BigDecimal("-33.897900"), new BigDecimal("151.179200"),
                buildStats(), buildTransport(), buildSchools(), List.of(),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private PagedSuburbResponse buildPage() {
        return new PagedSuburbResponse(List.of(buildSummary()), 0, 1, 1L, 20);
    }

    // ── GET /api/suburbs ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/suburbs")
    class ListAll {

        @Test
        @DisplayName("200 — returns paged suburb response")
        void returns200WithPage() throws Exception {
            when(suburbService.getAllSuburbs(0, 20, "name")).thenReturn(buildPage());

            mockMvc.perform(get("/api/suburbs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.suburbs[0].id").value(SUBURB_ID.toString()))
                    .andExpect(jsonPath("$.suburbs[0].name").value("Newtown"))
                    .andExpect(jsonPath("$.suburbs[0].postcode").value("2042"))
                    .andExpect(jsonPath("$.suburbs[0].region").value("INNER_WEST"));
        }

        @Test
        @DisplayName("200 — empty page when no suburbs exist")
        void returns200WithEmptyPage() throws Exception {
            when(suburbService.getAllSuburbs(0, 20, "name"))
                    .thenReturn(new PagedSuburbResponse(List.of(), 0, 0, 0L, 20));

            mockMvc.perform(get("/api/suburbs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.suburbs").isEmpty());
        }
    }

    // ── GET /api/suburbs/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/suburbs/{id}")
    class GetById {

        @Test
        @DisplayName("200 — returns full suburb detail with transport and schools at top level")
        void found_returns200() throws Exception {
            doReturn(buildDetail()).when(suburbService).findById(SUBURB_ID);

            mockMvc.perform(get("/api/suburbs/{id}", SUBURB_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBURB_ID.toString()))
                    .andExpect(jsonPath("$.name").value("Newtown"))
                    .andExpect(jsonPath("$.stats.medianRentWeekly").value(650))
                    .andExpect(jsonPath("$.stats.walkabilityAmenityCount").value(47))
                    .andExpect(jsonPath("$.transport.nearestTrainStation").value("Newtown Station"))
                    .andExpect(jsonPath("$.schoolData.avgIcseaScore").value(1087.50))
                    .andExpect(jsonPath("$.schoolData.dataAvailable").value(true));
        }

        @Test
        @DisplayName("404 — suburb not found")
        void notFound_returns404() throws Exception {
            when(suburbService.findById(SUBURB_ID))
                    .thenThrow(new ResourceNotFoundException("Suburb", SUBURB_ID.toString()));

            mockMvc.perform(get("/api/suburbs/{id}", SUBURB_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("400 — invalid UUID format")
        void invalidUuid_returns400() throws Exception {
            mockMvc.perform(get("/api/suburbs/not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/suburbs/postcode/{postcode} ──────────────────────────────────

    @Nested
    @DisplayName("GET /api/suburbs/postcode/{postcode}")
    class GetByPostcode {

        @Test
        @DisplayName("200 — returns matching suburbs")
        void found_returns200() throws Exception {
            when(suburbService.findByPostcode("2042")).thenReturn(List.of(buildDetail()));

            mockMvc.perform(get("/api/suburbs/postcode/{postcode}", "2042"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].postcode").value("2042"))
                    .andExpect(jsonPath("$[0].name").value("Newtown"));
        }

        @Test
        @DisplayName("404 — postcode not found")
        void notFound_returns404() throws Exception {
            when(suburbService.findByPostcode("9999"))
                    .thenThrow(new ResourceNotFoundException("Suburb", "postcode 9999"));

            mockMvc.perform(get("/api/suburbs/postcode/{postcode}", "9999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // ── GET /api/suburbs/{id}/stats ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/suburbs/{id}/stats")
    class GetStats {

        @Test
        @DisplayName("200 — returns suburb stats with all new fields")
        void found_returns200() throws Exception {
            doReturn(buildStats()).when(suburbService).getSuburbStats(SUBURB_ID);

            mockMvc.perform(get("/api/suburbs/{id}/stats", SUBURB_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.medianRentWeekly").value(650))
                    .andExpect(jsonPath("$.walkabilityAmenityCount").value(47))
                    .andExpect(jsonPath("$.parksCount").value(8))
                    .andExpect(jsonPath("$.pctHouses").value(45))
                    .andExpect(jsonPath("$.population").value(15420));
        }

        @Test
        @DisplayName("404 — suburb not found")
        void notFound_returns404() throws Exception {
            when(suburbService.getSuburbStats(SUBURB_ID))
                    .thenThrow(new ResourceNotFoundException("Suburb", SUBURB_ID.toString()));

            mockMvc.perform(get("/api/suburbs/{id}/stats", SUBURB_ID))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /api/suburbs/{id}/transport ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/suburbs/{id}/transport")
    class GetTransport {

        @Test
        @DisplayName("200 — returns transport data")
        void found_returns200() throws Exception {
            doReturn(buildTransport()).when(suburbService).getTransportData(SUBURB_ID);

            mockMvc.perform(get("/api/suburbs/{id}/transport", SUBURB_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nearestTrainStation").value("Newtown Station"))
                    .andExpect(jsonPath("$.trainStationWalkMins").value(5))
                    .andExpect(jsonPath("$.numBusRoutes").value(8));
        }
    }

    // ── GET /api/suburbs/{id}/schools ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/suburbs/{id}/schools")
    class GetSchools {

        @Test
        @DisplayName("200 — returns school data with ICSEA score and dataAvailable flag")
        void found_returns200() throws Exception {
            doReturn(buildSchools()).when(suburbService).getSchoolData(SUBURB_ID);

            mockMvc.perform(get("/api/suburbs/{id}/schools", SUBURB_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.numPrimarySchools").value(2))
                    .andExpect(jsonPath("$.avgIcseaScore").value(1087.50))
                    .andExpect(jsonPath("$.dataAvailable").value(true));
        }

        @Test
        @DisplayName("200 — returns notYetAvailable DTO (not 404) when data missing")
        void notYetAvailable_returns200WithFalseFlag() throws Exception {
            doReturn(SchoolDataResponse.notYetAvailable(SUBURB_ID))
                    .when(suburbService).getSchoolData(SUBURB_ID);

            mockMvc.perform(get("/api/suburbs/{id}/schools", SUBURB_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dataAvailable").value(false));
        }
    }

    // ── POST /api/suburbs/bulk-details ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/suburbs/bulk-details")
    class BulkDetails {

        @Test
        @DisplayName("200 — returns list of suburb details")
        void returns200() throws Exception {
            when(suburbService.getBulkSuburbDetails(anyList())).thenReturn(List.of(buildDetail()));

            mockMvc.perform(post("/api/suburbs/bulk-details")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(SUBURB_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(SUBURB_ID.toString()));
        }
    }

    // ── Saved Suburbs ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/suburbs/saved/{suburbId}")
    class SaveSuburb {

        @Test
        @DisplayName("200 — suburb saved successfully")
        void returns200() throws Exception {
            when(suburbService.saveSuburb(USER_ID, SUBURB_ID)).thenReturn(buildDetail());

            mockMvc.perform(post("/api/suburbs/saved/{suburbId}", SUBURB_ID)
                    .header("X-User-Id", USER_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBURB_ID.toString()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/suburbs/saved/{suburbId}")
    class UnsaveSuburb {

        @Test
        @DisplayName("204 — suburb removed from saved")
        void returns204() throws Exception {
            doNothing().when(suburbService).unsaveSuburb(USER_ID, SUBURB_ID);

            mockMvc.perform(delete("/api/suburbs/saved/{suburbId}", SUBURB_ID)
                    .header("X-User-Id", USER_ID.toString()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/suburbs/saved")
    class GetSaved {

        @Test
        @DisplayName("200 — returns saved suburbs for user")
        void returns200() throws Exception {
            when(suburbService.getSavedSuburbs(USER_ID)).thenReturn(List.of(buildDetail()));

            mockMvc.perform(get("/api/suburbs/saved")
                    .header("X-User-Id", USER_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(SUBURB_ID.toString()));
        }
    }
}
