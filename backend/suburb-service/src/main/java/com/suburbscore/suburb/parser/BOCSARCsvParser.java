package com.suburbscore.suburb.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses BOCSAR (NSW Bureau of Crime Statistics) crime CSV.
 * Download from: https://www.bocsar.nsw.gov.au/Pages/bocsar_crime_stats/bocsar_local_crime_tool.aspx
 *
 * Expected columns: suburb, offence_category, incidents_per_100k
 */
@Slf4j
@Component
public class BOCSARCsvParser {

    @Value("${data.dir:./data}")
    private String dataDir;

    private static final String FILE_NAME = "bocsar_crime.csv";

    // Returns map of suburb name (uppercase) → crime index (0–100)
    public Map<String, BigDecimal> parse() {
        Path filePath = Path.of(dataDir, FILE_NAME);
        if (!Files.exists(filePath)) {
            log.warn("BOCSAR crime file not found at {} — skipping crime index update", filePath);
            return Map.of();
        }

        Map<String, Double> rawTotals = new HashMap<>();
        Map<String, Integer> counts   = new HashMap<>();

        try (Reader reader = new FileReader(filePath.toFile())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                String suburb = record.get("suburb").toUpperCase().trim();
                String raw    = record.get("incidents_per_100k").trim();
                if (suburb.isBlank() || raw.isBlank()) continue;

                double value = Double.parseDouble(raw);
                rawTotals.merge(suburb, value, Double::sum);
                counts.merge(suburb, 1, Integer::sum);
            }
        } catch (Exception e) {
            log.error("Failed to parse BOCSAR CSV: {}", e.getMessage());
            return Map.of();
        }

        // Normalise to 0–100 scale (higher = more crime)
        double maxRaw = rawTotals.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        Map<String, BigDecimal> result = new HashMap<>();
        rawTotals.forEach((suburb, total) -> {
            double avg = total / counts.get(suburb);
            double normalised = (avg / maxRaw) * 100;
            result.put(suburb, BigDecimal.valueOf(normalised).setScale(2, RoundingMode.HALF_UP));
        });

        log.info("BOCSAR CSV parsed — {} suburbs with crime data", result.size());
        return result;
    }
}
