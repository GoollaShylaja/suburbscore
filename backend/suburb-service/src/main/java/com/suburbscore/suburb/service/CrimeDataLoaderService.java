package com.suburbscore.suburb.service;

import com.suburbscore.suburb.entity.Suburb;
import com.suburbscore.suburb.entity.SuburbStats;
import com.suburbscore.suburb.parser.BOCSARCsvParser;
import com.suburbscore.suburb.repository.SuburbRepository;
import com.suburbscore.suburb.repository.SuburbStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrimeDataLoaderService {

    private final BOCSARCsvParser       bocsarCsvParser;
    private final SuburbRepository      suburbRepository;
    private final SuburbStatsRepository suburbStatsRepository;

    @Transactional
    public int loadCrimeData() {
        Map<String, BigDecimal> crimeMap = bocsarCsvParser.parse();
        if (crimeMap.isEmpty()) {
            log.warn("BOCSAR parse returned no data — crime index not updated");
            return 0;
        }

        List<Suburb> suburbs = suburbRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        int updated = 0;

        for (Suburb suburb : suburbs) {
            BigDecimal crimeIndex = crimeMap.get(suburb.getSuburbName().toUpperCase());
            if (crimeIndex == null) continue;

            SuburbStats stats = suburbStatsRepository.findBySuburbId(suburb.getId())
                    .orElseGet(() -> {
                        SuburbStats s = new SuburbStats();
                        s.setSuburb(suburb);
                        return s;
                    });
            stats.setCrimeIndex(crimeIndex);
            stats.setCrimeUpdatedAt(now);
            suburbStatsRepository.save(stats);
            updated++;
        }

        log.info("Crime index updated for {} / {} suburbs", updated, suburbs.size());
        return updated;
    }
}
