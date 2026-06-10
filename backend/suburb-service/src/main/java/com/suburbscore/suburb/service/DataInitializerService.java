package com.suburbscore.suburb.service;

import com.suburbscore.suburb.client.NSWSpatialApiClient;
import com.suburbscore.suburb.entity.Region;
import com.suburbscore.suburb.entity.Suburb;
import com.suburbscore.suburb.enums.SydneyRegion;
import com.suburbscore.suburb.repository.RegionRepository;
import com.suburbscore.suburb.repository.SchoolDataRepository;
import com.suburbscore.suburb.repository.SuburbRepository;
import com.suburbscore.suburb.repository.SuburbStatsRepository;
import com.suburbscore.suburb.repository.TransportDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitializerService {

    private final SuburbRepository suburbRepository;
    private final SchoolDataRepository schoolDataRepository;
    private final TransportDataRepository transportDataRepository;
    private final SuburbStatsRepository suburbStatsRepository;
    private final RegionRepository regionRepository;
    private final NSWSpatialApiClient nswSpatialApiClient;
    private final SchoolDataLoaderService schoolDataLoaderService;
    private final TransportDataLoaderService transportDataLoaderService;
    private final WalkabilityDataLoaderService walkabilityDataLoaderService;
    private final CrimeDataLoaderService crimeDataLoaderService;
    private final TransportCorrectionService transportCorrectionService;
    private final SuburbPersistenceService suburbPersistenceService;

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        seedRegions();

        if (suburbRepository.count() == 0) {
            seedSuburbs();
        } else {
            log.info("Suburbs already populated ({} rows) — skipping seed", suburbRepository.count());
        }

        long activeSuburbs = suburbRepository.count(); // @SQLRestriction ensures only is_deleted=false

        long schoolCount = schoolDataRepository.countBySuburbIsDeletedFalseAndDataAvailableTrue();
        if (schoolCount == 0) {
            log.info("No usable school data found for active suburbs — triggering async load...");
            schoolDataLoaderService.loadForAllSuburbsAsync();
        } else {
            log.info("School data already populated ({} active suburbs) — skipping", schoolCount);
        }

        long transportCount = transportDataRepository.countForActiveSuburbs();
        if (transportCount < activeSuburbs) {
            log.info("Transport data incomplete ({}/{} active suburbs covered) — triggering async load...",
                    transportCount, activeSuburbs);
            transportDataLoaderService.loadForAllSuburbsAsync(); // corrections applied at end of async load
        } else {
            log.info("Transport data already populated ({} rows) — applying corrections", transportCount);
            transportCorrectionService.applyCorrections();
        }

        long walkabilityCount = suburbStatsRepository.countByParksCountIsNotNull();
        if (walkabilityCount == 0) {
            log.info("No walkability data found — triggering async load via OpenStreetMap...");
            walkabilityDataLoaderService.loadForAllSuburbsAsync();
        } else {
            log.info("Walkability data already populated ({} suburbs) — skipping", walkabilityCount);
        }

        long crimeCount = suburbStatsRepository.countByCrimeIndexIsNotNull();
        if (crimeCount == 0) {
            log.info("No crime data found — loading from BOCSAR CSV (last 2 years)...");
            crimeDataLoaderService.loadCrimeData();
        } else {
            log.info("Crime data already populated ({} suburbs) — skipping", crimeCount);
        }
    }

    private void seedRegions() {
        if (regionRepository.count() > 0) {
            log.info("Regions already seeded ({} rows) — skipping", regionRepository.count());
            return;
        }
        log.info("Seeding regions from SydneyRegion enum...");
        for (SydneyRegion sr : SydneyRegion.values()) {
            Region r = new Region();
            r.setCode(sr.name());
            r.setRegionName(toDisplayName(sr));
            r.setGreaterSydney(isGreaterSydney(sr));
            regionRepository.save(r);
        }
        log.info("Seeded {} regions", SydneyRegion.values().length);
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
            suburbPersistenceService.persistSuburbs(suburbs);
        } catch (Exception e) {
            log.error("NSW Spatial API failed — service will start without seed data: {}", e.getMessage());
        }
    }

    private static boolean isGreaterSydney(SydneyRegion sr) {
        return switch (sr) {
            case INNER_CITY, EASTERN_SUBURBS, INNER_WEST, NORTH_SHORE_RYDE, NORTHERN_BEACHES,
                 THE_HILLS_DISTRICT, WESTERN_SYDNEY, SOUTH_WESTERN_SYDNEY, MACARTHUR,
                 SUTHERLAND_ST_GEORGE, BLUE_MOUNTAINS, HAWKESBURY -> true;
            default -> false;
        };
    }

    private static String toDisplayName(SydneyRegion sr) {
        return switch (sr) {
            case INNER_CITY -> "Inner City";
            case EASTERN_SUBURBS -> "Eastern Suburbs";
            case INNER_WEST -> "Inner West";
            case NORTH_SHORE_RYDE -> "North Shore & Ryde";
            case NORTHERN_BEACHES -> "Northern Beaches";
            case THE_HILLS_DISTRICT -> "The Hills District";
            case WESTERN_SYDNEY -> "Western Sydney";
            case SOUTH_WESTERN_SYDNEY -> "South Western Sydney";
            case MACARTHUR -> "Macarthur";
            case SUTHERLAND_ST_GEORGE -> "Sutherland & St George";
            case BLUE_MOUNTAINS -> "Blue Mountains";
            case HAWKESBURY -> "Hawkesbury";
            case CENTRAL_COAST -> "Central Coast";
            case HUNTER_NEWCASTLE -> "Hunter & Newcastle";
            case NEW_ENGLAND -> "New England";
            case MID_NORTH_COAST -> "Mid North Coast";
            case NORTH_COAST -> "North Coast";
            case ILLAWARRA -> "Illawarra";
            case SOUTH_COAST -> "South Coast";
            case SOUTHERN_HIGHLANDS_GOULBURN -> "Southern Highlands & Goulburn";
            case SNOWY_MONARO -> "Snowy Monaro";
            case RIVERINA -> "Riverina";
            case CENTRAL_WEST -> "Central West";
            case OTHER_NSW -> "Other NSW";
        };
    }
}
