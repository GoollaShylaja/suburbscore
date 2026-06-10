package com.suburbscore.suburb.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
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
import java.util.List;
import java.util.Map;

/**
 * Parses BOCSAR (NSW Bureau of Crime Statistics) suburb data CSV.
 * Download "Suburb data" from: https://bocsar.nsw.gov.au/
 *
 * Expected format (wide pivot table):
 *   Suburb, Offence category, Subcategory, Jan 1995, Feb 1995, ... Dec 2025
 *
 * Only the last 2 calendar years of monthly columns are used. The year range
 * is detected dynamically from the header row — no hardcoded years.
 */
@Slf4j
@Component
public class BOCSARCsvParser {

    @Value("${data.dir:./data}")
    private String dataDir;

    @Value("${data.bocsar.file:SuburbData25Q4.csv}")
    private String fileName;

    private static final String MONTH_HEADER_PATTERN = "[A-Z][a-z]{2} \\d{4}";

    // Returns map of suburb name (uppercase) → crime index (0–100, higher = more crime)
    public Map<String, BigDecimal> parse() {
        Path filePath = Path.of(dataDir, fileName);
        if (!Files.exists(filePath)) {
            log.warn("BOCSAR crime file not found at {} — skipping crime index update", filePath);
            return Map.of();
        }

        Map<String, Long> suburbTotals = new HashMap<>();

        try (Reader reader = new FileReader(filePath.toFile());
             CSVParser csvParser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<String> selectedColumns = selectLastTwoYearColumns(csvParser.getHeaderNames());
            if (selectedColumns.isEmpty()) {
                log.warn("BOCSAR CSV: no monthly columns found in header — wrong file format?");
                return Map.of();
            }

            for (CSVRecord record : csvParser) {
                String suburb = record.get("Suburb").trim().toUpperCase();
                if (suburb.isBlank()) continue;

                long rowTotal = sumColumns(record, selectedColumns);
                suburbTotals.merge(suburb, rowTotal, Long::sum);
            }

        } catch (Exception e) {
            log.error("Failed to parse BOCSAR CSV: {}", e.getMessage());
            return Map.of();
        }

        if (suburbTotals.isEmpty()) return Map.of();

        Map<String, BigDecimal> result = normalise(suburbTotals);
        log.info("BOCSAR CSV parsed — {} suburbs with crime data", result.size());
        return result;
    }

    // ── Column selection ──────────────────────────────────────────────────────

    private List<String> selectLastTwoYearColumns(List<String> headers) {
        List<String> monthColumns = headers.stream()
                .filter(h -> h.matches(MONTH_HEADER_PATTERN))
                .toList();

        if (monthColumns.isEmpty()) return List.of();

        int maxYear = monthColumns.stream()
                .mapToInt(h -> Integer.parseInt(h.substring(4)))
                .max()
                .orElse(0);

        int cutoffYear = maxYear - 1; // include maxYear and the year before it
        List<String> selected = monthColumns.stream()
                .filter(h -> Integer.parseInt(h.substring(4)) >= cutoffYear)
                .toList();

        log.info("BOCSAR: file spans {} months total; using {} columns from {}-{} (last 2 years)",
                monthColumns.size(), selected.size(), cutoffYear, maxYear);
        return selected;
    }

    // ── Row summation ─────────────────────────────────────────────────────────

    private static long sumColumns(CSVRecord record, List<String> columns) {
        long total = 0;
        for (String col : columns) {
            try {
                String val = record.get(col).trim();
                if (!val.isBlank()) total += Long.parseLong(val);
            } catch (NumberFormatException ignored) {
                // suppressed/n.a. values treated as 0
            }
        }
        return total;
    }

    // ── Normalisation (0–100 scale) ───────────────────────────────────────────

    private static Map<String, BigDecimal> normalise(Map<String, Long> raw) {
        double maxTotal = raw.values().stream().mapToLong(Long::longValue).max().orElse(1);
        Map<String, BigDecimal> result = new HashMap<>();
        raw.forEach((suburb, total) -> {
            double normalised = (total / maxTotal) * 100.0;
            result.put(suburb, BigDecimal.valueOf(normalised).setScale(2, RoundingMode.HALF_UP));
        });
        return result;
    }
}
