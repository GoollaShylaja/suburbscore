package com.suburbscore.suburb.service;

import com.suburbscore.suburb.dto.*;
import com.suburbscore.suburb.entity.*;
import com.suburbscore.suburb.exception.ResourceNotFoundException;
import com.suburbscore.suburb.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuburbService")
class SuburbServiceTest {

    @Mock SuburbRepository suburbRepository;
    @Mock SuburbStatsRepository suburbStatsRepository;
    @Mock TransportDataRepository transportDataRepository;
    @Mock SchoolDataRepository schoolDataRepository;

    @InjectMocks SuburbService suburbService;

    private static final UUID SUBURB_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Suburb buildSuburb() {
        Suburb s = new Suburb();
        s.setId(SUBURB_ID);
        s.setName("Newtown");
        s.setPostcode("2042");
        s.setLga("Inner West Council");
        s.setRegion("Inner West");
        s.setLatitude(new BigDecimal("-33.897900"));
        s.setLongitude(new BigDecimal("151.179200"));
        s.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        s.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return s;
    }

    private SuburbStats buildStats(Suburb suburb) {
        SuburbStats st = new SuburbStats();
        st.setId(UUID.randomUUID());
        st.setSuburb(suburb);
        st.setMedianRentWeekly(650);
        st.setCrimeIndex(new BigDecimal("45.0"));
        st.setWalkabilityScore(new BigDecimal("88.0"));
        st.setPopulation(15420);
        st.setMedianAge(29);
        st.setUnemploymentRate(new BigDecimal("5.80"));
        st.setUpdatedAt(LocalDateTime.now());
        return st;
    }

    private TransportData buildTransport(Suburb suburb) {
        TransportData t = new TransportData();
        t.setId(UUID.randomUUID());
        t.setSuburb(suburb);
        t.setNearestTrainStation("Newtown Station");
        t.setTrainStationWalkMins(5);
        t.setNumBusRoutes(8);
        t.setCbdCommuteMinsTrain(18);
        t.setCbdCommuteMinsBus(25);
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    private SchoolData buildSchools(Suburb suburb) {
        SchoolData sc = new SchoolData();
        sc.setId(UUID.randomUUID());
        sc.setSuburb(suburb);
        sc.setNumPrimarySchools(2);
        sc.setNumHighSchools(1);
        sc.setAvgNaplanScore(new BigDecimal("520.0"));
        sc.setBestSchoolName("Newtown High School of the Performing Arts");
        sc.setUpdatedAt(LocalDateTime.now());
        return sc;
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns page of suburb summaries")
        void returnsPageOfSummaries() {
            Suburb suburb = buildSuburb();
            Page<Suburb> page = new PageImpl<>(List.of(suburb));
            when(suburbRepository.findAll(any(PageRequest.class))).thenReturn(page);

            Page<SuburbSummaryResponse> result = suburbService.findAll(PageRequest.of(0, 20));

            assertThat(result.getTotalElements()).isEqualTo(1);
            SuburbSummaryResponse summary = result.getContent().get(0);
            assertThat(summary.id()).isEqualTo(SUBURB_ID);
            assertThat(summary.name()).isEqualTo("Newtown");
            assertThat(summary.postcode()).isEqualTo("2042");
            assertThat(summary.region()).isEqualTo("Inner West");
        }

        @Test
        @DisplayName("returns empty page when no suburbs exist")
        void returnsEmptyPage() {
            when(suburbRepository.findAll(any(PageRequest.class))).thenReturn(Page.empty());

            Page<SuburbSummaryResponse> result = suburbService.findAll(PageRequest.of(0, 20));

            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns full detail when suburb exists with all related data")
        void found_returnsDetailResponse() {
            Suburb suburb = buildSuburb();
            when(suburbRepository.findById(SUBURB_ID)).thenReturn(Optional.of(suburb));
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildStats(suburb)));
            when(transportDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildTransport(suburb)));
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildSchools(suburb)));

            SuburbDetailResponse result = suburbService.findById(SUBURB_ID);

            assertThat(result.id()).isEqualTo(SUBURB_ID);
            assertThat(result.name()).isEqualTo("Newtown");
            assertThat(result.stats()).isNotNull();
            assertThat(result.stats().medianRentWeekly()).isEqualTo(650);
            assertThat(result.stats().transport()).isNotNull();
            assertThat(result.stats().transport().nearestTrainStation()).isEqualTo("Newtown Station");
            assertThat(result.stats().schools()).isNotNull();
            assertThat(result.stats().schools().avgNaplanScore()).isEqualByComparingTo("520.0");
        }

        @Test
        @DisplayName("returns detail with null nested data when no stats have been loaded yet")
        void found_missingRelatedData_returnsNullFields() {
            Suburb suburb = buildSuburb();
            when(suburbRepository.findById(SUBURB_ID)).thenReturn(Optional.of(suburb));
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(transportDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());

            SuburbDetailResponse result = suburbService.findById(SUBURB_ID);

            assertThat(result.id()).isEqualTo(SUBURB_ID);
            assertThat(result.stats().medianRentWeekly()).isNull();
            assertThat(result.stats().transport()).isNull();
            assertThat(result.stats().schools()).isNull();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when suburb does not exist")
        void notFound_throwsException() {
            when(suburbRepository.findById(SUBURB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> suburbService.findById(SUBURB_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Suburb not found");
        }
    }

    // ── findByPostcode ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByPostcode")
    class FindByPostcode {

        @Test
        @DisplayName("returns list of suburbs matching the postcode")
        void found_returnsList() {
            Suburb suburb = buildSuburb();
            when(suburbRepository.findByPostcodeOrderByNameAsc("2042")).thenReturn(List.of(suburb));
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildStats(suburb)));
            when(transportDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());

            List<SuburbDetailResponse> result = suburbService.findByPostcode("2042");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).postcode()).isEqualTo("2042");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no suburbs match the postcode")
        void notFound_throwsException() {
            when(suburbRepository.findByPostcodeOrderByNameAsc("9999")).thenReturn(List.of());

            assertThatThrownBy(() -> suburbService.findByPostcode("9999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("postcode 9999");
        }
    }

    // ── findStatsBySuburbId ───────────────────────────────────────────────────

    @Nested
    @DisplayName("findStatsBySuburbId")
    class FindStats {

        @Test
        @DisplayName("returns stats when suburb and all related data exist")
        void found_returnsStats() {
            Suburb suburb = buildSuburb();
            when(suburbRepository.existsById(SUBURB_ID)).thenReturn(true);
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildStats(suburb)));
            when(transportDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildTransport(suburb)));
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildSchools(suburb)));

            SuburbStatsResponse result = suburbService.findStatsBySuburbId(SUBURB_ID);

            assertThat(result.medianRentWeekly()).isEqualTo(650);
            assertThat(result.crimeIndex()).isEqualByComparingTo("45.0");
            assertThat(result.walkabilityScore()).isEqualByComparingTo("88.0");
            assertThat(result.transport().cbdCommuteMinsTrain()).isEqualTo(18);
            assertThat(result.schools().numHighSchools()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when suburb does not exist")
        void suburbNotFound_throwsException() {
            when(suburbRepository.existsById(SUBURB_ID)).thenReturn(false);

            assertThatThrownBy(() -> suburbService.findStatsBySuburbId(SUBURB_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Suburb not found");
        }

        @Test
        @DisplayName("returns response with null fields when no data has been ingested yet")
        void missingData_returnsNullFields() {
            when(suburbRepository.existsById(SUBURB_ID)).thenReturn(true);
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(transportDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());

            SuburbStatsResponse result = suburbService.findStatsBySuburbId(SUBURB_ID);

            assertThat(result.medianRentWeekly()).isNull();
            assertThat(result.transport()).isNull();
            assertThat(result.schools()).isNull();
        }
    }
}
