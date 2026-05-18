package com.suburbscore.suburb.service;

import com.suburbscore.suburb.client.NSWSpatialApiClient;
import com.suburbscore.suburb.entity.City;
import com.suburbscore.suburb.entity.Suburb;
import com.suburbscore.suburb.repository.CityRepository;
import com.suburbscore.suburb.repository.SchoolDataRepository;
import com.suburbscore.suburb.repository.SuburbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitializerService {

    private final SuburbRepository suburbRepository;
    private final SchoolDataRepository schoolDataRepository;
    private final CityRepository cityRepository;
    private final NSWSpatialApiClient nswSpatialApiClient;
    private final SchoolDataLoaderService schoolDataLoaderService;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        if (suburbRepository.count() == 0) {
            seedSuburbs();
        } else {
            log.info("Suburbs already populated ({} rows) — skipping seed", suburbRepository.count());
        }

        if (!schoolDataRepository.existsByDataAvailableTrue()) {
            log.info("No usable school data found — triggering async load...");
            schoolDataLoaderService.loadForAllSuburbsAsync();
        } else {
            log.info("School data already populated — skipping");
        }
    }

    private void seedSuburbs() {
        log.info("Suburbs table empty — seeding from NSW Spatial API...");
        try {
            List<Suburb> suburbs = nswSpatialApiClient.fetchAllNswSuburbs();
            if (suburbs.isEmpty()) {
                log.warn("NSW Spatial API returned no suburbs — service will start without seed data");
                return;
            }
            log.info("NSW Spatial API returned {} suburbs", suburbs.size());
            persistSuburbs(suburbs);
        } catch (Exception e) {
            log.error("NSW Spatial API failed — service will start without seed data: {}", e.getMessage());
        }
    }

    private void persistSuburbs(List<Suburb> suburbs) {
        Map<String, City> cityCache = new HashMap<>();

        for (Suburb suburb : suburbs) {
            String cityName = resolveCityName(suburb.getPostcode());
            City city = cityCache.computeIfAbsent(cityName, name ->
                    cityRepository.findByNameAndState(name, "NSW")
                            .orElseGet(() -> {
                                City c = new City();
                                c.setName(name);
                                c.setState("NSW");
                                return cityRepository.save(c);
                            }));
            suburb.setCity(city);
        }

        suburbRepository.saveAll(suburbs);
        log.info("Persisted {} suburbs across {} cities", suburbs.size(), cityCache.size());
        cityCache.forEach((name, city) -> log.info("  city='{}' id={}", name, city.getId()));
    }

    private static String resolveCityName(String postcodeStr) {
        try {
            int pc = Integer.parseInt(postcodeStr.trim());
            // Greater Sydney — inner/metro + outer west (Penrith/Blue Mtns/Hawkesbury) + Campbelltown/Camden
            if ((pc >= 2000 && pc <= 2239) || (pc >= 2555 && pc <= 2574) || (pc >= 2740 && pc <= 2786)) return "Sydney";
            if (pc >= 2250 && pc <= 2263) return "Central Coast";
            if (pc >= 2264 && pc <= 2399) return "Newcastle";
            if (pc >= 2400 && pc <= 2490) return "Mid North Coast";
            if (pc >= 2500 && pc <= 2599) return "Wollongong";
        } catch (NumberFormatException ignored) {}
        return "Regional NSW";
    }
}
