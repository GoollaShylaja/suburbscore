package com.suburbscore.suburb.service;

import com.suburbscore.suburb.config.TransportCorrectionsProperties;
import com.suburbscore.suburb.config.TransportCorrectionsProperties.Correction;
import com.suburbscore.suburb.entity.TransportData;
import com.suburbscore.suburb.repository.SuburbRepository;
import com.suburbscore.suburb.repository.TransportDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportCorrectionService {

    private final TransportCorrectionsProperties properties;
    private final SuburbRepository suburbRepository;
    private final TransportDataRepository transportDataRepository;

    /**
     * Applies manually-verified transport values from application.yml
     * ({@code transport.corrections}) to the database.
     *
     * Called on every startup so corrections stay current even when the
     * API data load is skipped (data already populated).
     */
    @Transactional
    public void applyCorrections() {
        if (properties.getCorrections().isEmpty()) return;

        int applied = 0;
        for (Correction c : properties.getCorrections()) {
            // Use postcode when provided to resolve duplicate suburb names precisely.
            var suburb = (c.getPostcode() != null)
                    ? suburbRepository.findBySuburbNameIgnoreCaseAndPostcode(c.getSuburb(), c.getPostcode()).orElse(null)
                    : suburbRepository.findBySuburbNameIgnoreCase(c.getSuburb()).orElse(null);

            if (suburb == null) {
                log.warn("Transport correction skipped — suburb not found: {} postcode={}",
                        c.getSuburb(), c.getPostcode());
                continue;
            }

            TransportData td = transportDataRepository.findBySuburbId(suburb.getId())
                    .orElseGet(() -> {
                        TransportData t = new TransportData();
                        t.setSuburb(suburb);
                        return t;
                    });

            if (c.getCbdCommuteMinsTrain() != null)  td.setCbdCommuteMinsTrain(c.getCbdCommuteMinsTrain());
            if (c.getNearestStation() != null)        td.setNearestTrainStation(c.getNearestStation());
            if (c.getTrainStationWalkMins() != null)  td.setTrainStationWalkMins(c.getTrainStationWalkMins());
            if (c.getHasFerryAccess() != null)        td.setHasFerryAccess(c.getHasFerryAccess());

            transportDataRepository.save(td);
            log.info("Transport correction applied — suburb={} postcode={} cbdTrain={} ferry={}",
                    c.getSuburb(), c.getPostcode(), c.getCbdCommuteMinsTrain(), c.getHasFerryAccess());
            applied++;
        }
        log.info("Transport corrections complete — {}/{} applied", applied, properties.getCorrections().size());
    }
}
