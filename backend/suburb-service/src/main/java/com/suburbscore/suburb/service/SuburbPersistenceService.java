package com.suburbscore.suburb.service;

import com.suburbscore.suburb.entity.City;
import com.suburbscore.suburb.entity.Region;
import com.suburbscore.suburb.entity.Suburb;
import com.suburbscore.suburb.enums.SydneyRegion;
import com.suburbscore.suburb.repository.CityRepository;
import com.suburbscore.suburb.repository.RegionRepository;
import com.suburbscore.suburb.repository.SuburbRepository;
import com.suburbscore.suburb.util.RegionClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuburbPersistenceService {

    private final SuburbRepository suburbRepository;
    private final CityRepository cityRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public void persistSuburbs(List<Suburb> suburbs) {
        Map<String, City> cityCache = new HashMap<>();
        Map<SydneyRegion, Region> regionCache = new HashMap<>();

        for (Suburb suburb : suburbs) {
            String cityName = resolveCityName(suburb.getPostcode());
            City city = cityCache.computeIfAbsent(cityName, name ->
                    cityRepository.findByCityNameAndState(name, "NSW")
                            .orElseGet(() -> {
                                City c = new City();
                                c.setCityName(name);
                                c.setState("NSW");
                                return cityRepository.save(c);
                            }));
            suburb.setCity(city);

            SydneyRegion sr = RegionClassifier.classify(suburb.getSuburbName(), suburb.getPostcode());
            Region region = regionCache.computeIfAbsent(sr, key ->
                    regionRepository.findByCode(key.name())
                            .orElseThrow(() -> new IllegalStateException("Region not seeded: " + key)));
            suburb.setRegion(region);
        }

        suburbRepository.saveAll(suburbs);
        log.info("Persisted {} suburbs across {} cities, {} regions",
                suburbs.size(), cityCache.size(), regionCache.size());
    }

    private static String resolveCityName(String postcodeStr) {
        try {
            int pc = Integer.parseInt(postcodeStr.trim());
            if ((pc >= 2000 && pc <= 2239) || (pc >= 2555 && pc <= 2574) || (pc >= 2740 && pc <= 2786)) return "Sydney";
            if (pc >= 2250 && pc <= 2263) return "Central Coast";
            if (pc >= 2264 && pc <= 2399) return "Newcastle";
            if (pc >= 2400 && pc <= 2490) return "Mid North Coast";
            if (pc >= 2500 && pc <= 2599) return "Wollongong";
        } catch (NumberFormatException ignored) {}
        return "Regional NSW";
    }
}
