package com.suburbscore.suburb.scheduler;

import com.suburbscore.suburb.client.OpenStreetMapApiClient;
import com.suburbscore.suburb.client.TransportNSWApiClient;
import com.suburbscore.suburb.entity.*;
import com.suburbscore.suburb.enums.PropertyType;
import com.suburbscore.suburb.kafka.SuburbDataUpdatedEvent;
import com.suburbscore.suburb.kafka.SuburbKafkaProducer;
import com.suburbscore.suburb.parser.BOCSARCsvParser;
import com.suburbscore.suburb.parser.NSWRentExcelParser;
import com.suburbscore.suburb.parser.PropertySalesCsvParser;
import com.suburbscore.suburb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataRefreshScheduler {

    private final SuburbRepository            suburbRepository;
    private final SuburbStatsRepository       suburbStatsRepository;
    private final TransportDataRepository     transportDataRepository;
    private final SuburbRentByTypeRepository  rentByTypeRepository;
    private final TransportNSWApiClient       transportNSWApiClient;
    private final OpenStreetMapApiClient      osmApiClient;
    private final BOCSARCsvParser             bocsarCsvParser;
    private final NSWRentExcelParser          nswRentExcelParser;
    private final PropertySalesCsvParser      propertySalesCsvParser;
    private final CacheManager                cacheManager;
    private final Optional<SuburbKafkaProducer> kafkaProducer; // optional — only if Kafka configured

    @Scheduled(cron = "0 0 2 * * SUN") // every Sunday at 2am
    public void weeklyRefresh() {
        log.info("=== Weekly data refresh started ===");
        long start = System.currentTimeMillis();

        List<Suburb> suburbs = suburbRepository.findAll();
        log.info("Refreshing data for {} suburbs", suburbs.size());

        refreshTransportData(suburbs);
        refreshWalkabilityData(suburbs);
        refreshCrimeData(suburbs);
        refreshRentData();
        refreshPropertyComposition(suburbs);

        evictAllCaches();

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        log.info("=== Weekly data refresh complete in {}s ===", elapsed);

        kafkaProducer.ifPresent(p ->
                p.publishDataUpdated(SuburbDataUpdatedEvent.fullRefresh(suburbs.size())));
    }

    // ── Transport NSW ─────────────────────────────────────────────────────────

    @Transactional
    public void refreshTransportData(List<Suburb> suburbs) {
        log.info("Refreshing transport data via Transport NSW API...");
        int updated = 0;

        for (Suburb suburb : suburbs) {
            try {
                TransportNSWApiClient.TransportResult result = transportNSWApiClient.fetchTransportData(
                        suburb.getName(), suburb.getLatitude(), suburb.getLongitude());

                if (result.nearestTrainStation() == null && result.numBusRoutes() == 0) continue;

                TransportData transport = transportDataRepository.findBySuburbId(suburb.getId())
                        .orElseGet(() -> {
                            TransportData t = new TransportData();
                            t.setSuburb(suburb);
                            return t;
                        });

                if (result.nearestTrainStation() != null)
                    transport.setNearestTrainStation(result.nearestTrainStation());
                transport.setTrainStationWalkMins(result.trainStationWalkMins());
                transport.setNumBusRoutes(result.numBusRoutes());
                transportDataRepository.save(transport);
                updated++;
            } catch (Exception e) {
                log.warn("Transport update failed for {}: {}", suburb.getName(), e.getMessage());
            }
        }
        log.info("Transport data refreshed for {} suburbs", updated);
    }

    // ── OpenStreetMap ─────────────────────────────────────────────────────────

    @Transactional
    public void refreshWalkabilityData(List<Suburb> suburbs) {
        log.info("Refreshing walkability data via OpenStreetMap...");
        int updated = 0;

        for (Suburb suburb : suburbs) {
            try {
                OpenStreetMapApiClient.OsmResult result = osmApiClient.fetchWalkabilityData(suburb.getName());
                if (result.parksCount() == 0 && result.amenityCount() == 0) continue;

                SuburbStats stats = suburbStatsRepository.findBySuburbId(suburb.getId())
                        .orElseGet(() -> {
                            SuburbStats s = new SuburbStats();
                            s.setSuburb(suburb);
                            return s;
                        });

                stats.setParksCount(result.parksCount());
                stats.setWalkabilityAmenityCount(result.amenityCount());
                suburbStatsRepository.save(stats);
                updated++;
            } catch (Exception e) {
                log.warn("OSM update failed for {}: {}", suburb.getName(), e.getMessage());
            }
        }
        log.info("Walkability data refreshed for {} suburbs", updated);
    }

    // ── BOCSAR Crime ──────────────────────────────────────────────────────────

    @Transactional
    public void refreshCrimeData(List<Suburb> suburbs) {
        log.info("Refreshing crime data from BOCSAR CSV...");
        Map<String, BigDecimal> crimeMap = bocsarCsvParser.parse();
        if (crimeMap.isEmpty()) return;

        int updated = 0;
        for (Suburb suburb : suburbs) {
            BigDecimal crimeIndex = crimeMap.get(suburb.getName().toUpperCase());
            if (crimeIndex == null) continue;

            SuburbStats stats = suburbStatsRepository.findBySuburbId(suburb.getId())
                    .orElseGet(() -> {
                        SuburbStats s = new SuburbStats();
                        s.setSuburb(suburb);
                        return s;
                    });
            stats.setCrimeIndex(crimeIndex);
            suburbStatsRepository.save(stats);
            updated++;
        }
        log.info("Crime index updated for {} suburbs", updated);
    }

    // ── NSW Rent ──────────────────────────────────────────────────────────────

    @Transactional
    public void refreshRentData() {
        log.info("Refreshing rent data from NSW DCJ Excel...");
        List<NSWRentExcelParser.RentRecord> rentRecords = nswRentExcelParser.parse();
        if (rentRecords.isEmpty()) return;

        int updated = 0;
        for (NSWRentExcelParser.RentRecord record : rentRecords) {
            try {
                List<Suburb> matches = suburbRepository.findByPostcodeOrderByNameAsc(record.postcode());
                for (Suburb suburb : matches) {
                    SuburbRentByType rent = rentByTypeRepository
                            .findBySuburbIdAndBedroomsAndPropertyType(
                                    suburb.getId(), record.bedrooms(), record.propertyType())
                            .orElseGet(() -> {
                                SuburbRentByType r = new SuburbRentByType();
                                r.setSuburb(suburb);
                                r.setBedrooms(record.bedrooms());
                                r.setPropertyType(record.propertyType());
                                return r;
                            });
                    rent.setMedianRentWeekly(record.medianRentWeekly());
                    rentByTypeRepository.save(rent);
                    updated++;
                }
            } catch (Exception e) {
                log.warn("Rent update failed for postcode {}: {}", record.postcode(), e.getMessage());
            }
        }
        log.info("Rent data updated for {} suburb-bedroom-type combinations", updated);
    }

    // ── Property Composition ──────────────────────────────────────────────────

    @Transactional
    public void refreshPropertyComposition(List<Suburb> suburbs) {
        log.info("Refreshing property composition from NSW Valuer General CSV...");
        Map<String, PropertySalesCsvParser.PropertyComposition> compositionMap = propertySalesCsvParser.parse();
        if (compositionMap.isEmpty()) return;

        int updated = 0;
        for (Suburb suburb : suburbs) {
            PropertySalesCsvParser.PropertyComposition comp =
                    compositionMap.get(suburb.getName().toUpperCase());
            if (comp == null) continue;

            SuburbStats stats = suburbStatsRepository.findBySuburbId(suburb.getId())
                    .orElseGet(() -> {
                        SuburbStats s = new SuburbStats();
                        s.setSuburb(suburb);
                        return s;
                    });
            stats.setPctHouses(comp.pctHouses());
            stats.setPctApartments(comp.pctApartments());
            stats.setPctTownhouses(comp.pctTownhouses());
            stats.setPctUnits(comp.pctUnits());
            suburbStatsRepository.save(stats);
            updated++;
        }
        log.info("Property composition updated for {} suburbs", updated);
    }

    // ── Cache Eviction ────────────────────────────────────────────────────────

    private void evictAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        log.info("All suburb caches evicted after refresh");
    }
}
