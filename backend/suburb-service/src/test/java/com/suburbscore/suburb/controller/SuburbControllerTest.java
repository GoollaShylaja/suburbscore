package com.suburbscore.suburb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbscore.suburb.config.SecurityConfig;
import com.suburbscore.suburb.dto.*;
import com.suburbscore.suburb.exception.GlobalExceptionHandler;
import com.suburbscore.suburb.exception.ResourceNotFoundException;
import com.suburbscore.suburb.security.JwtAuthenticationFilter;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private static final UUID SUBURB_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private SuburbSummaryResponse buildSummary() {
        return new SuburbSummaryResponse(
                SUBURB_ID, "Newtown", "2042", "Inner West Council", "Inner West",
                new BigDecimal("-33.897900"), new BigDecimal("151.179200"));
    }

    private TransportDataResponse buildTransport() {
        return new TransportDataResponse(
                "Newtown Station", 5, 8, 18, 25, LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private SchoolDataResponse buildSchools() {
        return new SchoolDataResponse(2, 1, new BigDecimal("520.0"),
                "Newtown High School of the Performing Arts", LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private SuburbStatsResponse buildStats() {
        return new SuburbStatsResponse(
                650, new BigDecimal("45.0"), new BigDecimal("88.0"),
                15420, 29, new BigDecimal("5.80"),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                buildTransport(), buildSchools());
    }

    private SuburbDetailResponse buildDetail() {
        return new SuburbDetailResponse(
                SUBURB_ID, "Newtown", "2042", "Inner West Council", "Inner West",
                new BigDecimal("-33.897900"), new BigDecimal("151.179200"),
                buildStats(),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    // ── GET /api/suburbs ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/suburbs")
    class ListAll {

        @Test
        @DisplayName("200 — returns paginated suburb list")
        void returns200WithPage() throws Exception {
            when(suburbService.findAll(any())).thenReturn(
                    new PageImpl<>(List.of(buildSummary()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/suburbs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(SUBURB_ID.toString()))
                    .andExpect(jsonPath("$.content[0].name").value("Newtown"))
                    .andExpect(jsonPath("$.content[0].postcode").value("2042"))
                    .andExpect(jsonPath("$.content[0].region").value("Inner West"));
        }

        @Test
        @DisplayName("200 — empty page when no suburbs exist")
        void returns200WithEmptyPage() throws Exception {
            when(suburbService.findAll(any())).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/suburbs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ── GET /api/suburbs/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/suburbs/{id}")
    class GetById {

        @Test
        @DisplayName("200 — returns full suburb detail")
        void found_returns200() throws Exception {
            doReturn(buildDetail()).when(suburbService).findById(SUBURB_ID);

            mockMvc.perform(get("/api/suburbs/{id}", SUBURB_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBURB_ID.toString()))
                    .andExpect(jsonPath("$.name").value("Newtown"))
                    .andExpect(jsonPath("$.postcode").value("2042"))
                    .andExpect(jsonPath("$.stats.medianRentWeekly").value(650))
                    .andExpect(jsonPath("$.stats.crimeIndex").value(45.0))
                    .andExpect(jsonPath("$.stats.transport.nearestTrainStation").value("Newtown Station"))
                    .andExpect(jsonPath("$.stats.transport.cbdCommuteMinsTrain").value(18))
                    .andExpect(jsonPath("$.stats.schools.avgNaplanScore").value(520.0))
                    .andExpect(jsonPath("$.stats.schools.bestSchoolName")
                            .value("Newtown High School of the Performing Arts"));
        }

        @Test
        @DisplayName("404 — suburb not found")
        void notFound_returns404() throws Exception {
            when(suburbService.findById(SUBURB_ID))
                    .thenThrow(new ResourceNotFoundException("Suburb", SUBURB_ID.toString()));

            mockMvc.perform(get("/api/suburbs/{id}", SUBURB_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Suburb not found: " + SUBURB_ID));
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
        @DisplayName("200 — returns multiple suburbs for shared postcode")
        void sharedPostcode_returnsMultiple() throws Exception {
            SuburbDetailResponse stLeonards = new SuburbDetailResponse(
                    UUID.randomUUID(), "St Leonards", "2065", "North Sydney Council", "North Shore",
                    new BigDecimal("-33.822700"), new BigDecimal("151.194000"),
                    buildStats(), LocalDateTime.now(), LocalDateTime.now());
            SuburbDetailResponse crowsNest = new SuburbDetailResponse(
                    UUID.randomUUID(), "Crows Nest", "2065", "North Sydney Council", "North Shore",
                    new BigDecimal("-33.826700"), new BigDecimal("151.206400"),
                    buildStats(), LocalDateTime.now(), LocalDateTime.now());

            when(suburbService.findByPostcode("2065")).thenReturn(List.of(stLeonards, crowsNest));

            mockMvc.perform(get("/api/suburbs/postcode/{postcode}", "2065"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("404 — postcode not found")
        void notFound_returns404() throws Exception {
            when(suburbService.findByPostcode("9999"))
                    .thenThrow(new ResourceNotFoundException("Suburb", "postcode 9999"));

            mockMvc.perform(get("/api/suburbs/postcode/{postcode}", "9999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Suburb not found: postcode 9999"));
        }
    }

    // ── GET /api/suburbs/{id}/stats ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/suburbs/{id}/stats")
    class GetStats {

        @Test
        @DisplayName("200 — returns full stats response")
        void found_returns200() throws Exception {
            doReturn(buildStats()).when(suburbService).findStatsBySuburbId(SUBURB_ID);

            mockMvc.perform(get("/api/suburbs/{id}/stats", SUBURB_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.medianRentWeekly").value(650))
                    .andExpect(jsonPath("$.walkabilityScore").value(88.0))
                    .andExpect(jsonPath("$.population").value(15420))
                    .andExpect(jsonPath("$.medianAge").value(29))
                    .andExpect(jsonPath("$.unemploymentRate").value(5.80))
                    .andExpect(jsonPath("$.transport.numBusRoutes").value(8))
                    .andExpect(jsonPath("$.transport.cbdCommuteMinsBus").value(25))
                    .andExpect(jsonPath("$.schools.numPrimarySchools").value(2))
                    .andExpect(jsonPath("$.schools.numHighSchools").value(1));
        }

        @Test
        @DisplayName("404 — suburb not found")
        void notFound_returns404() throws Exception {
            when(suburbService.findStatsBySuburbId(SUBURB_ID))
                    .thenThrow(new ResourceNotFoundException("Suburb", SUBURB_ID.toString()));

            mockMvc.perform(get("/api/suburbs/{id}/stats", SUBURB_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
