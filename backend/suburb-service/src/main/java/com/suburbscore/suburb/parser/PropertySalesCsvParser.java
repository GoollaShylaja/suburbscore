package com.suburbscore.suburb.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses NSW Valuer General property sales CSV.
 * Download from: https://valuation.property.nsw.gov.au/embed/propertySalesInformation
 *
 * Expected columns: suburb, property_type
 * property_type values: RESIDENTIAL HOUSE, RESIDENTIAL FLAT, RESIDENTIAL TOWNHOUSE, RESIDENTIAL UNIT
 */
@Slf4j
@Component
public class PropertySalesCsvParser {

    @Value("${data.dir:./data}")
    private String dataDir;

    private static final String FILE_NAME = "nsw_property_sales.csv";

    public record PropertyComposition(int houses, int apartments,
                                      int townhouses, int units, int total) {
        public int pctHouses()     { return total == 0 ? 0 : (int) Math.round(houses * 100.0 / total); }
        public int pctApartments() { return total == 0 ? 0 : (int) Math.round(apartments * 100.0 / total); }
        public int pctTownhouses() { return total == 0 ? 0 : (int) Math.round(townhouses * 100.0 / total); }
        public int pctUnits()      { return total == 0 ? 0 : (int) Math.round(units * 100.0 / total); }
    }

    // Returns map of suburb name (uppercase) → PropertyComposition
    public Map<String, PropertyComposition> parse() {
        Path filePath = Path.of(dataDir, FILE_NAME);
        if (!Files.exists(filePath)) {
            log.warn("Property sales CSV not found at {} — skipping property composition update", filePath);
            return Map.of();
        }

        // suburb → [houses, apartments, townhouses, units]
        Map<String, int[]> counts = new HashMap<>();

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
                String type   = record.get("property_type").toUpperCase().trim();
                if (suburb.isBlank()) continue;

                counts.putIfAbsent(suburb, new int[4]);
                int[] c = counts.get(suburb);
                if (type.contains("HOUSE"))     c[0]++;
                else if (type.contains("FLAT") || type.contains("APART")) c[1]++;
                else if (type.contains("TOWN")) c[2]++;
                else if (type.contains("UNIT")) c[3]++;
            }
        } catch (Exception e) {
            log.error("Failed to parse property sales CSV: {}", e.getMessage());
            return Map.of();
        }

        Map<String, PropertyComposition> result = new HashMap<>();
        counts.forEach((suburb, c) -> {
            int total = c[0] + c[1] + c[2] + c[3];
            if (total > 0) result.put(suburb, new PropertyComposition(c[0], c[1], c[2], c[3], total));
        });

        log.info("Property sales CSV parsed — {} suburbs with composition data", result.size());
        return result;
    }
}
