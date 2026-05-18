package com.suburbscore.suburb.service;

import com.suburbscore.suburb.dto.*;
import com.suburbscore.suburb.entity.*;
import com.suburbscore.suburb.enums.PropertyType;
import com.suburbscore.suburb.exception.ResourceNotFoundException;
import com.suburbscore.suburb.exception.SuburbNotFoundException;
import com.suburbscore.suburb.repository.*;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuburbService")
class SuburbServiceTest {

    @Mock SuburbRepository suburbRepository;
    @Mock SuburbStatsRepository suburbStatsRepository;
    @Mock TransportDataRepository transportDataRepository;
    @Mock SchoolDataRepository schoolDataRepository;
    @Mock SuburbRentByTypeRepository rentByTypeRepository;
    @Mock SavedSuburbRepository savedSuburbRepository;

    @InjectMocks SuburbService suburbService;

    private static final UUID SUBURB_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Suburb buildSuburb() {
        Suburb s = new Suburb();
        s.setId(SUBURB_ID);
        s.setName("Newtown");
        s.setPostcode("2042");
        s.setLga("Inner West Council");
        s.setRegion("INNER_WEST");
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
        st.setWalkabilityAmenityCount(47);
        st.setParksCount(8);
        st.setPctHouses(45);
        st.setPctApartments(38);
        st.setPctTownhouses(10);
        st.setPctUnits(7);
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
        sc.setAvgIcseaScore(new BigDecimal("1087.50"));
        sc.setBestSchoolName("Newtown High School of the Performing Arts");
        sc.setDataAvailable(true);
        sc.setUpdatedAt(LocalDateTime.now());
        return sc;
    }

    // ── getAllSuburbs ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllSuburbs")
    class GetAllSuburbs {

        @Test
        @DisplayName("returns paged response with suburb summaries")
        void returnsPagedResponse() {
            Page<Suburb> page = new PageImpl<>(List.of(buildSuburb()), PageRequest.of(0, 20), 1);
            when(suburbRepository.findAll(any(PageRequest.class))).thenReturn(page);

            PagedSuburbResponse result = suburbService.getAllSuburbs(0, 20, "name");

            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.currentPage()).isZero();
            assertThat(result.suburbs().get(0).name()).isEqualTo("Newtown");
        }

        @Test
        @DisplayName("returns empty paged response when no suburbs exist")
        void returnsEmptyPage() {
            when(suburbRepository.findAll(any(PageRequest.class))).thenReturn(Page.empty());

            PagedSuburbResponse result = suburbService.getAllSuburbs(0, 20, "name");

            assertThat(result.totalElements()).isZero();
        }
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns full detail with all related data")
        void found_returnsDetailResponse() {
            Suburb suburb = buildSuburb();
            when(suburbRepository.findById(SUBURB_ID)).thenReturn(Optional.of(suburb));
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildStats(suburb)));
            when(transportDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildTransport(suburb)));
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildSchools(suburb)));
            when(rentByTypeRepository.findBySuburbId(SUBURB_ID)).thenReturn(List.of());

            SuburbDetailResponse result = suburbService.findById(SUBURB_ID);

            assertThat(result.id()).isEqualTo(SUBURB_ID);
            assertThat(result.stats().medianRentWeekly()).isEqualTo(650);
            assertThat(result.transport().nearestTrainStation()).isEqualTo("Newtown Station");
            assertThat(result.schoolData().avgIcseaScore()).isEqualByComparingTo("1087.50");
            assertThat(result.schoolData().dataAvailable()).isTrue();
        }

        @Test
        @DisplayName("returns empty DTOs when no related data loaded yet")
        void found_missingRelatedData_returnsEmptyDTOs() {
            Suburb suburb = buildSuburb();
            when(suburbRepository.findById(SUBURB_ID)).thenReturn(Optional.of(suburb));
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(transportDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(rentByTypeRepository.findBySuburbId(SUBURB_ID)).thenReturn(List.of());

            SuburbDetailResponse result = suburbService.findById(SUBURB_ID);

            assertThat(result.stats().medianRentWeekly()).isNull();
            assertThat(result.transport().nearestTrainStation()).isNull();
            assertThat(result.schoolData().dataAvailable()).isFalse();
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
        @DisplayName("returns suburbs for valid postcode")
        void found_returnsList() {
            Suburb suburb = buildSuburb();
            when(suburbRepository.findByPostcodeOrderByNameAsc("2042")).thenReturn(List.of(suburb));
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildStats(suburb)));
            when(transportDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(rentByTypeRepository.findBySuburbId(SUBURB_ID)).thenReturn(List.of());

            List<SuburbDetailResponse> result = suburbService.findByPostcode("2042");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).postcode()).isEqualTo("2042");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no suburbs match postcode")
        void notFound_throwsException() {
            when(suburbRepository.findByPostcodeOrderByNameAsc("9999")).thenReturn(List.of());

            assertThatThrownBy(() -> suburbService.findByPostcode("9999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("postcode 9999");
        }
    }

    // ── getSuburbStats ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSuburbStats")
    class GetStats {

        @Test
        @DisplayName("returns stats with all new fields when data exists")
        void found_returnsStats() {
            Suburb suburb = buildSuburb();
            when(suburbRepository.existsById(SUBURB_ID)).thenReturn(true);
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildStats(suburb)));

            SuburbStatsResponse result = suburbService.getSuburbStats(SUBURB_ID);

            assertThat(result.medianRentWeekly()).isEqualTo(650);
            assertThat(result.walkabilityAmenityCount()).isEqualTo(47);
            assertThat(result.parksCount()).isEqualTo(8);
            assertThat(result.pctHouses()).isEqualTo(45);
        }

        @Test
        @DisplayName("returns empty DTO when stats not yet loaded")
        void missingData_returnsEmptyDTO() {
            when(suburbRepository.existsById(SUBURB_ID)).thenReturn(true);
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());

            SuburbStatsResponse result = suburbService.getSuburbStats(SUBURB_ID);

            assertThat(result.suburbId()).isEqualTo(SUBURB_ID);
            assertThat(result.medianRentWeekly()).isNull();
        }

        @Test
        @DisplayName("throws SuburbNotFoundException when suburb does not exist")
        void suburbNotFound_throwsException() {
            when(suburbRepository.existsById(SUBURB_ID)).thenReturn(false);

            assertThatThrownBy(() -> suburbService.getSuburbStats(SUBURB_ID))
                    .isInstanceOf(SuburbNotFoundException.class);
        }
    }

    // ── getSchoolData ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSchoolData")
    class GetSchoolData {

        @Test
        @DisplayName("returns school data with dataAvailable=true when loaded")
        void found_returnsSchoolData() {
            Suburb suburb = buildSuburb();
            when(suburbRepository.existsById(SUBURB_ID)).thenReturn(true);
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.of(buildSchools(suburb)));

            SchoolDataResponse result = suburbService.getSchoolData(SUBURB_ID);

            assertThat(result.dataAvailable()).isTrue();
            assertThat(result.numPrimarySchools()).isEqualTo(2);
            assertThat(result.avgIcseaScore()).isEqualByComparingTo("1087.50");
        }

        @Test
        @DisplayName("returns notYetAvailable DTO when school data not loaded")
        void notAvailable_returnsDefaultDTO() {
            when(suburbRepository.existsById(SUBURB_ID)).thenReturn(true);
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());

            SchoolDataResponse result = suburbService.getSchoolData(SUBURB_ID);

            assertThat(result.dataAvailable()).isFalse();
            assertThat(result.numPrimarySchools()).isZero();
        }
    }

    // ── getRentByBedroomsAndType ──────────────────────────────────────────────

    @Nested
    @DisplayName("getRentByBedroomsAndType")
    class GetRentByType {

        @Test
        @DisplayName("returns rent DTO when record exists")
        void found_returnsRentDTO() {
            Suburb suburb = buildSuburb();
            SuburbRentByType rent = SuburbRentByType.builder()
                    .id(UUID.randomUUID()).suburb(suburb)
                    .bedrooms(2).propertyType(PropertyType.APARTMENT)
                    .medianRentWeekly(620).updatedAt(LocalDateTime.now())
                    .build();
            when(rentByTypeRepository.findBySuburbIdAndBedroomsAndPropertyType(
                    SUBURB_ID, 2, PropertyType.APARTMENT)).thenReturn(Optional.of(rent));

            var result = suburbService.getRentByBedroomsAndType(SUBURB_ID, 2, PropertyType.APARTMENT);

            assertThat(result).isPresent();
            assertThat(result.get().medianRentWeekly()).isEqualTo(620);
            assertThat(result.get().propertyType()).isEqualTo("APARTMENT");
        }

        @Test
        @DisplayName("returns empty Optional when record not found")
        void notFound_returnsEmpty() {
            when(rentByTypeRepository.findBySuburbIdAndBedroomsAndPropertyType(
                    SUBURB_ID, 5, PropertyType.STUDIO)).thenReturn(Optional.empty());

            var result = suburbService.getRentByBedroomsAndType(SUBURB_ID, 5, PropertyType.STUDIO);

            assertThat(result).isEmpty();
        }
    }

    // ── getBulkSuburbDetails ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getBulkSuburbDetails")
    class GetBulkDetails {

        @Test
        @DisplayName("returns details for found IDs and skips missing ones")
        void returnsFoundSuburbs() {
            Suburb suburb = buildSuburb();
            UUID missing = UUID.randomUUID();
            when(suburbRepository.findById(SUBURB_ID)).thenReturn(Optional.of(suburb));
            when(suburbRepository.findById(missing)).thenReturn(Optional.empty());
            when(suburbStatsRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(transportDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(schoolDataRepository.findBySuburbId(SUBURB_ID)).thenReturn(Optional.empty());
            when(rentByTypeRepository.findBySuburbId(SUBURB_ID)).thenReturn(List.of());

            List<SuburbDetailResponse> result = suburbService.getBulkSuburbDetails(List.of(SUBURB_ID, missing));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(SUBURB_ID);
        }
    }
}
