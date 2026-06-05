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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolDataLoaderService {

    private static final int   MAX_SCHOOLS_PER_SUBURB = 15;
    private static final double RADIUS_TIGHT_KM        = 5.0;
    private static final double RADIUS_MID_KM          = 15.0;
    private static final double RADIUS_WIDE_KM         = 30.0;
    private static final int   TIGHT_THRESHOLD         = 3;  // expand past 5km if fewer than this

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
        Map<UUID, List<NSWSchoolResponse>> bySuburbId = buildSuburbSchoolMap(allSchools, suburbs);

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
    public void reloadAllAsync() {
        log.info("Reloading school data for all suburbs...");
        // Do not call loadForAllSuburbsAsync() here — self-invocation bypasses the @Async proxy.
        List<NSWSchoolResponse> allSchools = nswSchoolsApiClient.fetchAllSchools();
        if (allSchools.isEmpty()) {
            log.warn("No school data retrieved from NSW API — aborting reload");
            return;
        }

        List<Suburb> suburbs = suburbRepository.findAll();
        Map<UUID, List<NSWSchoolResponse>> bySuburbId = buildSuburbSchoolMap(allSchools, suburbs);

        int processed = 0;
        for (Suburb suburb : suburbs) {
            List<NSWSchoolResponse> schools = bySuburbId.getOrDefault(suburb.getId(), List.of());
            processSchoolData(suburb, schools);
            processed++;
            if (processed % 50 == 0) {
                log.info("School reload progress: {}/{} suburbs", processed, suburbs.size());
            }
        }
        log.info("School data reload complete — {} suburbs processed", processed);
    }

    /**
     * For each suburb, collects the nearest schools using a tiered radius:
     *   5 km  — if ≥ 3 schools found, stop here (dense urban case)
     *  15 km  — if ≥ 1 school found, stop here
     *  30 km  — last resort for sparse regional suburbs
     * Results are capped at MAX_SCHOOLS_PER_SUBURB nearest schools, preventing dense-city
     * suburbs from aggregating hundreds of schools across a wide metro area.
     */
    private Map<UUID, List<NSWSchoolResponse>> buildSuburbSchoolMap(
            List<NSWSchoolResponse> allSchools, List<Suburb> suburbs) {

        record ParsedSchool(NSWSchoolResponse school, double lat, double lon) {}
        record Candidate(NSWSchoolResponse school, double distKm) {}

        List<ParsedSchool> geoSchools = new ArrayList<>();
        List<NSWSchoolResponse> nameOnlySchools = new ArrayList<>();

        for (NSWSchoolResponse school : allSchools) {
            Double lat = parseCoord(school.latitude());
            Double lon = parseCoord(school.longitude());
            if (lat != null && lon != null) {
                geoSchools.add(new ParsedSchool(school, lat, lon));
            } else {
                nameOnlySchools.add(school);
            }
        }
        if (!nameOnlySchools.isEmpty()) {
            log.warn("{} schools had no coordinates — will use suburb name matching as fallback",
                    nameOnlySchools.size());
        }

        Map<UUID, List<NSWSchoolResponse>> bySuburbId = new HashMap<>();

        for (Suburb suburb : suburbs) {
            List<NSWSchoolResponse> nearby = new ArrayList<>();

            if (suburb.getLatitude() != null && suburb.getLongitude() != null) {
                double subLat = suburb.getLatitude().doubleValue();
                double subLon = suburb.getLongitude().doubleValue();

                // Compute distance to every school once, sorted nearest-first
                List<Candidate> byDist = geoSchools.stream()
                        .map(ps -> new Candidate(ps.school(),
                                haversineKm(subLat, subLon, ps.lat(), ps.lon())))
                        .sorted(Comparator.comparingDouble(Candidate::distKm))
                        .toList();

                // Pick the tightest radius that still yields enough schools
                long within5 = byDist.stream().filter(c -> c.distKm() <= RADIUS_TIGHT_KM).count();
                double radius;
                if (within5 >= TIGHT_THRESHOLD) {
                    radius = RADIUS_TIGHT_KM;
                } else {
                    long within15 = byDist.stream().filter(c -> c.distKm() <= RADIUS_MID_KM).count();
                    radius = (within15 >= 1) ? RADIUS_MID_KM : RADIUS_WIDE_KM;
                }

                byDist.stream()
                        .filter(c -> c.distKm() <= radius)
                        .limit(MAX_SCHOOLS_PER_SUBURB)
                        .map(Candidate::school)
                        .forEach(nearby::add);
            }

            // Name-matching fallback for schools with no coordinates
            String suburbNameLower = suburb.getSuburbName().trim().toLowerCase();
            for (NSWSchoolResponse school : nameOnlySchools) {
                if (school.suburb() != null && school.suburb().trim().equalsIgnoreCase(suburbNameLower)) {
                    nearby.add(school);
                }
            }

            if (!nearby.isEmpty()) {
                bySuburbId.put(suburb.getId(), nearby);
            }
        }

        return bySuburbId;
    }

    @Transactional
    public void processSchoolData(Suburb suburb, List<NSWSchoolResponse> schools) {
        try {
            if (schools.isEmpty()) {
                log.debug("No schools found for suburb {}, skipping insert", suburb.getSuburbName());
                return;
            }

            long primary = schools.stream()
                    .filter(s -> s.schoolType() != null && isPrimarySchool(s.schoolType()))
                    .count();
            long secondary = schools.stream()
                    .filter(s -> s.schoolType() != null && isSecondarySchool(s.schoolType()))
                    .count();

            // Best school's ICSEA — not an average. Averaging across all nearby schools
            // dilutes selective/high-performing schools toward the NSW mean (~950).
            OptionalDouble bestIcsea = schools.stream()
                    .map(s -> parseIcsea(s.icseaValue()))
                    .filter(v -> v != null && v > 0)
                    .mapToDouble(Double::doubleValue)
                    .max();

            String bestSchool = schools.stream()
                    .filter(s -> parseIcsea(s.icseaValue()) != null)
                    .max(Comparator.comparingDouble(s -> parseIcsea(s.icseaValue())))
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
            data.setBestIcseaScore(bestIcsea.isPresent()
                    ? BigDecimal.valueOf(bestIcsea.getAsDouble()).setScale(0, RoundingMode.HALF_UP)
                    : null);
            data.setBestSchoolName(bestSchool);
            data.setDataAvailable(true);

            schoolDataRepository.save(data);
        } catch (Exception e) {
            log.error("Failed to process school data for suburb {}: {}", suburb.getSuburbName(), e.getMessage());
        }
    }

    private static boolean isPrimarySchool(String level) {
        String l = level.toLowerCase();
        return l.contains("primary") || l.contains("infants")
                || l.contains("central") || l.contains("community");
    }

    private static boolean isSecondarySchool(String level) {
        String l = level.toLowerCase();
        return l.contains("secondary") || l.contains("central") || l.contains("community");
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
