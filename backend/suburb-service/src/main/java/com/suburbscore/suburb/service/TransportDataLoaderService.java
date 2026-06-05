package com.suburbscore.suburb.service;

import com.suburbscore.suburb.client.TransportNSWApiClient;
import com.suburbscore.suburb.entity.Suburb;
import com.suburbscore.suburb.entity.TransportData;
import com.suburbscore.suburb.repository.SuburbRepository;
import com.suburbscore.suburb.repository.TransportDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportDataLoaderService {

    private final SuburbRepository suburbRepository;
    private final TransportDataRepository transportDataRepository;
    private final TransportNSWApiClient transportNSWApiClient;
    private final TransportCorrectionService transportCorrectionService;

    @Async("asyncExecutor")
    public void loadForAllSuburbsAsync() {
        log.info("Starting async transport data load via Transport NSW API...");
        List<Suburb> suburbs = suburbRepository.findAll();
        int processed = 0;
        int noData = 0;

        for (Suburb suburb : suburbs) {
            try {
                Thread.sleep(200); // respect Transport NSW rate limit (~5 req/sec)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Transport data load interrupted at suburb {}", suburb.getSuburbName());
                break;
            }

            TransportNSWApiClient.TransportResult result = transportNSWApiClient.fetchTransportData(
                    suburb.getSuburbName(), suburb.getLatitude(), suburb.getLongitude());

            if (!result.hasAnyData()) {
                noData++;
            }
            // Always persist a record — even all-null — so count() reflects full coverage
            // and DataInitializerService doesn't re-trigger on every restart.
            processTransportData(suburb, result);

            processed++;
            if (processed % 50 == 0) {
                log.info("Transport data progress: {}/{} suburbs processed ({} returned no data)",
                        processed, suburbs.size(), noData);
            }
        }
        log.info("Transport data load complete — {}/{} suburbs processed, {} had no API data",
                processed, suburbs.size(), noData);
        transportCorrectionService.applyCorrections();
    }

    @Async("asyncExecutor")
    public void reloadAllAsync() {
        log.info("Reloading transport data for all suburbs...");
        // Do not call loadForAllSuburbsAsync() here — self-invocation bypasses the
        // @Async proxy. Duplicate the loop logic directly in this async method instead.
        List<Suburb> suburbs = suburbRepository.findAll();
        int processed = 0;
        int noData = 0;

        for (Suburb suburb : suburbs) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Reload interrupted at suburb {}", suburb.getSuburbName());
                break;
            }

            TransportNSWApiClient.TransportResult result = transportNSWApiClient.fetchTransportData(
                    suburb.getSuburbName(), suburb.getLatitude(), suburb.getLongitude());

            if (!result.hasAnyData()) {
                noData++;
            }
            processTransportData(suburb, result);

            processed++;
            if (processed % 50 == 0) {
                log.info("Reload progress: {}/{} suburbs ({} no data)", processed, suburbs.size(), noData);
            }
        }
        log.info("Reload complete — {}/{} suburbs, {} had no API data", processed, suburbs.size(), noData);
        transportCorrectionService.applyCorrections();
    }

    @Transactional
    public void processTransportData(Suburb suburb, TransportNSWApiClient.TransportResult result) {
        try {
            TransportData td = transportDataRepository.findBySuburbId(suburb.getId())
                    .orElseGet(() -> {
                        TransportData t = new TransportData();
                        t.setSuburb(suburb);
                        return t;
                    });

            if (result.nearestTrainStation() != null) {
                td.setNearestTrainStation(result.nearestTrainStation());
            }
            td.setTrainStationWalkMins(result.trainStationWalkMins());
            td.setNumBusRoutes(result.numBusRoutes());
            td.setHasFerryAccess(result.hasFerryAccess());

            if (result.cbdCommuteMinsTrain() != null) {
                td.setCbdCommuteMinsTrain(result.cbdCommuteMinsTrain());
            }
            if (result.cbdCommuteMinsBus() != null) {
                td.setCbdCommuteMinsBus(result.cbdCommuteMinsBus());
            }

            transportDataRepository.save(td);
            log.debug("Saved transport data for suburb {} — station={}, walkMins={}, bus={}, ferry={}, cbdTrain={}",
                    suburb.getSuburbName(), result.nearestTrainStation(), result.trainStationWalkMins(),
                    result.numBusRoutes(), result.hasFerryAccess(), result.cbdCommuteMinsTrain());

        } catch (Exception e) {
            log.error("Failed to save transport data for suburb {}: {}", suburb.getSuburbName(), e.getMessage());
        }
    }
}
