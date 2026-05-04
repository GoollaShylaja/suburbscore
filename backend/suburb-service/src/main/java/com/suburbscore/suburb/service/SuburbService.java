package com.suburbscore.suburb.service;

import com.suburbscore.suburb.dto.*;
import com.suburbscore.suburb.entity.*;
import com.suburbscore.suburb.exception.ResourceNotFoundException;
import com.suburbscore.suburb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuburbService {

    private final SuburbRepository suburbRepository;
    private final SuburbStatsRepository suburbStatsRepository;
    private final TransportDataRepository transportDataRepository;
    private final SchoolDataRepository schoolDataRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "suburbs", key = "'page:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<SuburbSummaryResponse> findAll(Pageable pageable) {
        return suburbRepository.findAll(pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "suburb", key = "#id")
    public SuburbDetailResponse findById(UUID id) {
        Suburb suburb = suburbRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suburb", id.toString()));
        return toDetail(suburb);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "suburbsByPostcode", key = "#postcode")
    public List<SuburbDetailResponse> findByPostcode(String postcode) {
        List<Suburb> results = suburbRepository.findByPostcodeOrderByNameAsc(postcode);
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Suburb", "postcode " + postcode);
        }
        return results.stream().map(this::toDetail).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "suburbStats", key = "#id")
    public SuburbStatsResponse findStatsBySuburbId(UUID id) {
        if (!suburbRepository.existsById(id)) {
            throw new ResourceNotFoundException("Suburb", id.toString());
        }
        SuburbStats stats = suburbStatsRepository.findBySuburbId(id).orElse(null);
        TransportData transport = transportDataRepository.findBySuburbId(id).orElse(null);
        SchoolData schools = schoolDataRepository.findBySuburbId(id).orElse(null);
        return toStatsResponse(stats, transport, schools);
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
        return new SuburbDetailResponse(
                s.getId(), s.getName(), s.getPostcode(), s.getLga(), s.getRegion(),
                s.getLatitude(), s.getLongitude(),
                toStatsResponse(stats, transport, schools),
                s.getCreatedAt(), s.getUpdatedAt());
    }

    private SuburbStatsResponse toStatsResponse(SuburbStats stats, TransportData transport, SchoolData schools) {
        return new SuburbStatsResponse(
                stats != null ? stats.getMedianRentWeekly() : null,
                stats != null ? stats.getCrimeIndex() : null,
                stats != null ? stats.getWalkabilityScore() : null,
                stats != null ? stats.getPopulation() : null,
                stats != null ? stats.getMedianAge() : null,
                stats != null ? stats.getUnemploymentRate() : null,
                stats != null ? stats.getUpdatedAt() : null,
                transport != null ? toTransportResponse(transport) : null,
                schools != null ? toSchoolsResponse(schools) : null);
    }

    private TransportDataResponse toTransportResponse(TransportData t) {
        return new TransportDataResponse(
                t.getNearestTrainStation(),
                t.getTrainStationWalkMins(),
                t.getNumBusRoutes(),
                t.getCbdCommuteMinsTrain(),
                t.getCbdCommuteMinsBus(),
                t.getUpdatedAt());
    }

    private SchoolDataResponse toSchoolsResponse(SchoolData s) {
        return new SchoolDataResponse(
                s.getNumPrimarySchools(),
                s.getNumHighSchools(),
                s.getAvgNaplanScore(),
                s.getBestSchoolName(),
                s.getUpdatedAt());
    }
}
