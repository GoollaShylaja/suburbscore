package com.suburbscore.suburb.service;

import com.suburbscore.suburb.client.NSWSchoolResponse;
import com.suburbscore.suburb.client.NSWSchoolsApiClient;
import com.suburbscore.suburb.entity.SchoolData;
import com.suburbscore.suburb.entity.Suburb;
import com.suburbscore.suburb.repository.SchoolDataRepository;
import com.suburbscore.suburb.repository.SuburbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolDataLoaderService {

    private static final double MAX_RADIUS_KM = 15.0;

    private final SuburbRepository suburbRepository;
    private final SchoolDataRepository schoolDataRepository;
    private final NSWSchoolsApiClient nswSchoolsApiClient;

    @Async("asyncExecutor")
    public void loadForAllSuburbsAsync() {
        log.info("Starting async school data load — bulk fetching from NSW API...");

        List<NSWSchoolResponse> allSchools = nswSchoolsApiClient.fetchAllSchools();
        if (allSchools.isEmpty()) {
            log.warn("No school data retrieved from NSW API — aborting load");
            return;
        }

        List<Suburb> suburbs = suburbRepository.findAll();

        // Assign each school to its nearest suburb centroid within MAX_RADIUS_KM.
        // Falls back to name-matching for schools that have no coordinates.
        Map<UUID, List<NSWSchoolResponse>> bySuburbId = new HashMap<>();
        int noCoords = 0;
        for (NSWSchoolResponse school : allSchools) {
            Double lat = parseCoord(school.latitude());
            Double lon = parseCoord(school.longitude());
            if (lat != null && lon != null) {
                Suburb nearest = findNearestSuburb(suburbs, lat, lon);
                if (nearest != null) {
                    bySuburbId.computeIfAbsent(nearest.getId(), k -> new ArrayList<>()).add(school);
                }
            } else {
                noCoords++;
                if (school.suburb() != null && !school.suburb().isBlank()) {
                    String nameKey = school.suburb().trim().toLowerCase();
                    suburbs.stream()
                            .filter(s -> s.getName().trim().equalsIgnoreCase(nameKey))
                            .findFirst()
                            .ifPresent(s -> bySuburbId.computeIfAbsent(s.getId(), k -> new ArrayList<>()).add(school));
                }
            }
        }
        if (noCoords > 0) {
            log.warn("{} schools had no coordinates — used name matching as fallback", noCoords);
        }

        int processed = 0;
        for (Suburb suburb : suburbs) {
            List<NSWSchoolResponse> schools = bySuburbId.getOrDefault(suburb.getId(), List.of());
            processSchoolData(suburb, schools);
            processed++;
            if (processed % 50 == 0) {
                log.info("School data progress: {}/{} suburbs processed", processed, suburbs.size());
            }
        }
        log.info("School data load complete — processed {} suburbs", processed);
    }

    @Async("asyncExecutor")
    @Transactional
    public void reloadAllAsync() {
        log.info("Reloading school data for all suburbs...");
        loadForAllSuburbsAsync();
    }

    @Transactional
    public void processSchoolData(Suburb suburb, List<NSWSchoolResponse> schools) {
        try {
            if (schools.isEmpty()) {
                log.debug("No schools found for suburb {}, skipping insert", suburb.getName());
                return;
            }

            long primary = schools.stream()
                    .filter(s -> s.schoolType() != null &&
                            (s.schoolType().toLowerCase().contains("primary") ||
                             s.schoolType().toLowerCase().contains("infants")))
                    .count();
            long secondary = schools.stream()
                    .filter(s -> s.schoolType() != null &&
                            s.schoolType().toLowerCase().contains("secondary"))
                    .count();

            OptionalDouble avgIcsea = schools.stream()
                    .map(s -> parseIcsea(s.icseaValue()))
                    .filter(v -> v != null && v > 0)
                    .mapToDouble(Double::doubleValue)
                    .average();

            // Prefer highest-ICSEA school; fall back to any named school when ICSEA is absent
            String bestSchool = schools.stream()
                    .filter(s -> parseIcsea(s.icseaValue()) != null)
                    .max((a, b) -> Double.compare(
                            parseIcsea(a.icseaValue()),
                            parseIcsea(b.icseaValue())))
                    .map(NSWSchoolResponse::schoolName)
                    .or(() -> schools.stream()
                            .filter(s -> s.schoolName() != null && !s.schoolName().isBlank())
                            .map(NSWSchoolResponse::schoolName)
                            .findFirst())
                    .orElse(null);

            SchoolData data = schoolDataRepository.findBySuburbId(suburb.getId())
                    .orElseGet(() -> {
                        SchoolData sd = new SchoolData();
                        sd.setSuburb(suburb);
                        return sd;
                    });

            data.setNumPrimarySchools((int) primary);
            data.setNumHighSchools((int) secondary);
            data.setAvgIcseaScore(avgIcsea.isPresent()
                    ? BigDecimal.valueOf(avgIcsea.getAsDouble()).setScale(0, RoundingMode.HALF_UP)
                    : null);
            data.setBestSchoolName(bestSchool);
            data.setDataAvailable(true);

            schoolDataRepository.save(data);
        } catch (Exception e) {
            log.error("Failed to process school data for suburb {}: {}", suburb.getName(), e.getMessage());
        }
    }

    private Suburb findNearestSuburb(List<Suburb> suburbs, double schoolLat, double schoolLon) {
        Suburb nearest = null;
        double minDist = MAX_RADIUS_KM;
        for (Suburb s : suburbs) {
            if (s.getLatitude() == null || s.getLongitude() == null) continue;
            double dist = haversineKm(schoolLat, schoolLon,
                    s.getLatitude().doubleValue(), s.getLongitude().doubleValue());
            if (dist < minDist) {
                minDist = dist;
                nearest = s;
            }
        }
        return nearest;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Double parseCoord(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseIcsea(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
