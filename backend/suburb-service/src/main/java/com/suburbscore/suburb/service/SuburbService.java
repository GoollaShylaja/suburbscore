package com.suburbscore.suburb.service;

import com.suburbscore.suburb.client.PostcodeApiClient;
import com.suburbscore.suburb.client.PostcodeApiResponse;
import com.suburbscore.suburb.dto.*;
import com.suburbscore.suburb.entity.*;
import com.suburbscore.suburb.enums.PropertyType;
import com.suburbscore.suburb.exception.BusinessException;
import com.suburbscore.suburb.exception.ResourceNotFoundException;
import com.suburbscore.suburb.exception.SuburbNotFoundException;
import com.suburbscore.suburb.repository.*;
import com.suburbscore.suburb.util.RegionClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuburbService {

    private final SuburbRepository suburbRepository;
    private final SuburbStatsRepository suburbStatsRepository;
    private final TransportDataRepository transportDataRepository;
    private final SchoolDataRepository schoolDataRepository;
    private final SuburbRentByTypeRepository rentByTypeRepository;
    private final SavedSuburbRepository savedSuburbRepository;
    private final PostcodeApiClient postcodeApiClient;

    // ── List / Search ─────────────────────────────────────────────────────────

    @Cacheable(value = "suburb:list", key = "#page + ':' + #size + ':' + #sort")
    public PagedSuburbResponse getAllSuburbs(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        Page<Suburb> result = suburbRepository.findAll(pageable);
        return new PagedSuburbResponse(
                result.getContent().stream().map(this::toSummary).toList(),
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements(),
                result.getSize()
        );
    }

    @Cacheable(value = "suburb:list", key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<SuburbSummaryResponse> findAll(Pageable pageable) {
        return suburbRepository.findAll(pageable).map(this::toSummary);
    }

    @Cacheable(value = "suburb:region", key = "#region")
    public List<SuburbSummaryResponse> getSuburbsByRegion(String region) {
        List<Suburb> suburbs = suburbRepository.findByRegionIgnoreCase(region);
        if (suburbs.isEmpty()) {
            log.warn("No suburbs found for region: {}", region);
        }
        return suburbs.stream().map(this::toSummary).toList();
    }

    @Cacheable(value = "suburb:postcode", key = "#postcode")
    public List<SuburbDetailResponse> findByPostcode(String postcode) {
        List<Suburb> results = suburbRepository.findByPostcodeOrderByNameAsc(postcode);
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Suburb", "postcode " + postcode);
        }
        return results.stream().map(this::toDetail).toList();
    }

    @Cacheable(value = "suburb:search", key = "#name")
    public List<SuburbSummaryResponse> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Search name must not be blank");
        }
        return suburbRepository.searchByName(name).stream().map(this::toSummary).toList();
    }

    // ── Single Suburb ─────────────────────────────────────────────────────────

    @Cacheable(value = "suburb:detail", key = "#id")
    public SuburbDetailResponse findById(UUID id) {
        Suburb suburb = suburbRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suburb", id.toString()));
        return toDetail(suburb);
    }

    @Cacheable(value = "suburb:stats", key = "#id")
    public SuburbStatsResponse getSuburbStats(UUID id) {
        if (!suburbRepository.existsById(id)) {
            throw new SuburbNotFoundException(id);
        }
        return suburbStatsRepository.findBySuburbId(id)
                .map(s -> toStatsResponse(s, s.getSuburb().getId()))
                .orElse(SuburbStatsResponse.empty(id));
    }

    @Cacheable(value = "suburb:stats", key = "#id")
    public SuburbStatsResponse findStatsBySuburbId(UUID id) {
        return getSuburbStats(id);
    }

    @Cacheable(value = "suburb:transport", key = "#id")
    public TransportDataResponse getTransportData(UUID id) {
        if (!suburbRepository.existsById(id)) {
            throw new SuburbNotFoundException(id);
        }
        return transportDataRepository.findBySuburbId(id)
                .map(t -> toTransportResponse(t, id))
                .orElse(TransportDataResponse.empty(id));
    }

    @Cacheable(value = "suburb:schools", key = "#id")
    public SchoolDataResponse getSchoolData(UUID id) {
        if (!suburbRepository.existsById(id)) {
            throw new SuburbNotFoundException(id);
        }
        return schoolDataRepository.findBySuburbId(id)
                .map(s -> toSchoolResponse(s, id))
                .orElse(SchoolDataResponse.notYetAvailable(id));
    }

    @Cacheable(value = "suburb:rent", key = "#suburbId")
    public List<SuburbRentDTO> getRentData(UUID suburbId) {
        if (!suburbRepository.existsById(suburbId)) {
            throw new SuburbNotFoundException(suburbId);
        }
        return rentByTypeRepository.findBySuburbId(suburbId).stream()
                .map(this::toRentDTO)
                .toList();
    }

    public Optional<SuburbRentDTO> getRentByBedroomsAndType(UUID suburbId, Integer bedrooms, PropertyType type) {
        return rentByTypeRepository
                .findBySuburbIdAndBedroomsAndPropertyType(suburbId, bedrooms, type)
                .map(this::toRentDTO);
    }

    public List<SuburbDetailResponse> getBulkSuburbDetails(List<UUID> suburbIds) {
        return suburbIds.stream()
                .map(id -> suburbRepository.findById(id).map(this::toDetail).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // ── Update (called by data-ingestion-service) ─────────────────────────────

    @Transactional
    @CacheEvict(value = {"suburb:stats", "suburb:detail"}, key = "#id")
    public SuburbStatsResponse updateStats(UUID id, SuburbStatsResponse dto) {
        Suburb suburb = suburbRepository.findById(id)
                .orElseThrow(() -> new SuburbNotFoundException(id));

        SuburbStats stats = suburbStatsRepository.findBySuburbId(id).orElseGet(() -> {
            SuburbStats s = new SuburbStats();
            s.setSuburb(suburb);
            return s;
        });

        if (dto.medianRentWeekly() != null)        stats.setMedianRentWeekly(dto.medianRentWeekly());
        if (dto.crimeIndex() != null)              stats.setCrimeIndex(dto.crimeIndex());
        if (dto.walkabilityScore() != null)        stats.setWalkabilityScore(dto.walkabilityScore());
        if (dto.walkabilityAmenityCount() != null) stats.setWalkabilityAmenityCount(dto.walkabilityAmenityCount());
        if (dto.parksCount() != null)              stats.setParksCount(dto.parksCount());
        if (dto.pctHouses() != null)               stats.setPctHouses(dto.pctHouses());
        if (dto.pctApartments() != null)           stats.setPctApartments(dto.pctApartments());
        if (dto.pctTownhouses() != null)           stats.setPctTownhouses(dto.pctTownhouses());
        if (dto.pctUnits() != null)                stats.setPctUnits(dto.pctUnits());
        if (dto.population() != null)              stats.setPopulation(dto.population());
        if (dto.medianAge() != null)               stats.setMedianAge(dto.medianAge());
        if (dto.unemploymentRate() != null)        stats.setUnemploymentRate(dto.unemploymentRate());

        suburbStatsRepository.save(stats);
        log.info("Updated stats for suburb {}", id);
        return toStatsResponse(stats, id);
    }

    @Transactional
    @CacheEvict(value = {"suburb:transport", "suburb:detail"}, key = "#id")
    public TransportDataResponse updateTransportData(UUID id, TransportDataResponse dto) {
        Suburb suburb = suburbRepository.findById(id)
                .orElseThrow(() -> new SuburbNotFoundException(id));

        TransportData transport = transportDataRepository.findBySuburbId(id).orElseGet(() -> {
            TransportData t = new TransportData();
            t.setSuburb(suburb);
            return t;
        });

        if (dto.nearestTrainStation() != null)   transport.setNearestTrainStation(dto.nearestTrainStation());
        if (dto.trainStationWalkMins() != null)  transport.setTrainStationWalkMins(dto.trainStationWalkMins());
        if (dto.numBusRoutes() != null)          transport.setNumBusRoutes(dto.numBusRoutes());
        if (dto.cbdCommuteMinsTrain() != null)   transport.setCbdCommuteMinsTrain(dto.cbdCommuteMinsTrain());
        if (dto.cbdCommuteMinsBus() != null)     transport.setCbdCommuteMinsBus(dto.cbdCommuteMinsBus());

        transportDataRepository.save(transport);
        log.info("Updated transport data for suburb {}", id);
        return toTransportResponse(transport, id);
    }

    @Transactional
    @CacheEvict(value = {"suburb:schools", "suburb:detail"}, key = "#id")
    public SchoolDataResponse updateSchoolData(UUID id, SchoolDataResponse dto) {
        Suburb suburb = suburbRepository.findById(id)
                .orElseThrow(() -> new SuburbNotFoundException(id));

        SchoolData schoolData = schoolDataRepository.findBySuburbId(id).orElseGet(() -> {
            SchoolData sd = new SchoolData();
            sd.setSuburb(suburb);
            return sd;
        });

        if (dto.numPrimarySchools() != null) schoolData.setNumPrimarySchools(dto.numPrimarySchools());
        if (dto.numHighSchools() != null)    schoolData.setNumHighSchools(dto.numHighSchools());
        if (dto.avgIcseaScore() != null)     schoolData.setAvgIcseaScore(dto.avgIcseaScore());
        if (dto.bestSchoolName() != null)    schoolData.setBestSchoolName(dto.bestSchoolName());
        if (dto.dataAvailable() != null)     schoolData.setDataAvailable(dto.dataAvailable());

        schoolDataRepository.save(schoolData);
        log.info("Updated school data for suburb {}", id);
        return toSchoolResponse(schoolData, id);
    }

    @Transactional
    @CacheEvict(value = {"suburb:rent", "suburb:detail"}, key = "#id")
    public SuburbRentDTO upsertRentData(UUID id, SuburbRentDTO dto) {
        Suburb suburb = suburbRepository.findById(id)
                .orElseThrow(() -> new SuburbNotFoundException(id));

        PropertyType type = PropertyType.valueOf(dto.propertyType().toUpperCase());

        SuburbRentByType rent = rentByTypeRepository
                .findBySuburbIdAndBedroomsAndPropertyType(id, dto.bedrooms(), type)
                .orElseGet(() -> SuburbRentByType.builder()
                        .suburb(suburb)
                        .bedrooms(dto.bedrooms())
                        .propertyType(type)
                        .build());

        rent.setMedianRentWeekly(dto.medianRentWeekly());
        rentByTypeRepository.save(rent);
        log.info("Upserted rent data for suburb {} bedrooms={} type={}", id, dto.bedrooms(), type);
        return toRentDTO(rent);
    }

    // ── Saved Suburbs ─────────────────────────────────────────────────────────

    @Transactional
    public SuburbDetailResponse saveSuburb(UUID userId, UUID suburbId) {
        Suburb suburb = suburbRepository.findById(suburbId)
                .orElseThrow(() -> new SuburbNotFoundException(suburbId));

        if (savedSuburbRepository.existsByUserIdAndSuburbId(userId, suburbId)) {
            throw new BusinessException("Suburb already saved");
        }

        SavedSuburb saved = SavedSuburb.builder()
                .userId(userId)
                .suburb(suburb)
                .build();
        savedSuburbRepository.save(saved);
        log.info("User {} saved suburb {}", userId, suburbId);
        return toDetail(suburb);
    }

    @Transactional
    public void unsaveSuburb(UUID userId, UUID suburbId) {
        SavedSuburb saved = savedSuburbRepository.findByUserIdAndSuburbId(userId, suburbId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved suburb", suburbId.toString()));
        savedSuburbRepository.delete(saved);
        log.info("User {} removed suburb {} from saved", userId, suburbId);
    }

    public List<SuburbDetailResponse> getSavedSuburbs(UUID userId) {
        return savedSuburbRepository.findByUserId(userId).stream()
                .map(s -> toDetail(s.getSuburb()))
                .toList();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private SuburbSummaryResponse toSummary(Suburb s) {
        return new SuburbSummaryResponse(
                s.getId(), s.getName(), s.getPostcode(),
                s.getLga(), s.getRegion(), s.getLatitude(), s.getLongitude());
    }

    private SuburbDetailResponse toDetail(Suburb s) {
        SuburbStats stats = suburbStatsRepository.findBySuburbId(s.getId()).orElse(null);
        TransportData transport = transportDataRepository.findBySuburbId(s.getId()).orElse(null);
        SchoolData schools = schoolDataRepository.findBySuburbId(s.getId()).orElse(null);
        List<SuburbRentDTO> rent = rentByTypeRepository.findBySuburbId(s.getId()).stream()
                .map(this::toRentDTO).toList();

        return new SuburbDetailResponse(
                s.getId(), s.getName(), s.getPostcode(), s.getLga(), s.getRegion(),
                s.getLatitude(), s.getLongitude(),
                stats != null ? toStatsResponse(stats, s.getId()) : SuburbStatsResponse.empty(s.getId()),
                transport != null ? toTransportResponse(transport, s.getId()) : TransportDataResponse.empty(s.getId()),
                schools != null ? toSchoolResponse(schools, s.getId()) : SchoolDataResponse.notYetAvailable(s.getId()),
                rent,
                s.getCreatedAt(), s.getUpdatedAt());
    }

    private SuburbStatsResponse toStatsResponse(SuburbStats s, UUID suburbId) {
        return new SuburbStatsResponse(
                suburbId,
                s.getMedianRentWeekly(),
                s.getCrimeIndex(),
                s.getWalkabilityScore(),
                s.getWalkabilityAmenityCount(),
                s.getParksCount(),
                s.getPctHouses(),
                s.getPctApartments(),
                s.getPctTownhouses(),
                s.getPctUnits(),
                s.getPopulation(),
                s.getMedianAge(),
                s.getUnemploymentRate(),
                s.getUpdatedAt());
    }

    private TransportDataResponse toTransportResponse(TransportData t, UUID suburbId) {
        return new TransportDataResponse(
                suburbId,
                t.getNearestTrainStation(),
                t.getTrainStationWalkMins(),
                t.getNumBusRoutes(),
                t.getCbdCommuteMinsTrain(),
                t.getCbdCommuteMinsBus(),
                t.getUpdatedAt());
    }

    private SchoolDataResponse toSchoolResponse(SchoolData s, UUID suburbId) {
        return new SchoolDataResponse(
                suburbId,
                s.getNumPrimarySchools(),
                s.getNumHighSchools(),
                s.getAvgIcseaScore(),
                s.getBestSchoolName(),
                s.getDataAvailable(),
                s.getUpdatedAt());
    }

    private SuburbRentDTO toRentDTO(SuburbRentByType r) {
        return new SuburbRentDTO(
                r.getSuburb().getId(),
                r.getBedrooms(),
                r.getPropertyType().name(),
                r.getMedianRentWeekly(),
                r.getUpdatedAt());
    }

    // ── Admin: on-demand postcode import ─────────────────────────────────────

    @Transactional
    @CacheEvict(value = {"suburb:list", "suburb:postcode"}, allEntries = true)
    public int importPostcode(int postcode) {
        List<PostcodeApiResponse> apiResults = postcodeApiClient.fetchByPostcode(postcode);
        if (apiResults.isEmpty()) {
            return 0;
        }

        String postcodeStr = String.valueOf(postcode);
        int added = 0;
        for (PostcodeApiResponse r : apiResults) {
            String name = toTitleCase(r.name());
            if (suburbRepository.existsByPostcodeAndNameIgnoreCase(postcodeStr, name)) {
                log.debug("Skipping existing suburb: {} {}", postcodeStr, name);
                continue;
            }
            Suburb s = new Suburb();
            s.setName(name);
            s.setPostcode(postcodeStr);
            s.setLatitude(BigDecimal.valueOf(r.latitude()));
            s.setLongitude(BigDecimal.valueOf(r.longitude()));
            s.setRegion(RegionClassifier.classify(postcodeStr).name());
            suburbRepository.save(s);
            added++;
        }

        log.info("Imported {}/{} new suburb(s) for postcode {}", added, apiResults.size(), postcode);
        return added;
    }

    private String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
