package com.suburbscore.suburb.service;

import com.suburbscore.suburb.entity.Suburb;
import com.suburbscore.suburb.util.RegionClassifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SuburbCsvLoader {

    @Value("${data.dir:./data}")
    private String dataDir;

    public List<Suburb> load() {
        Path csvPath = Path.of(dataDir, "suburbs.csv");
        log.info("Loading NSW suburbs from {}", csvPath.toAbsolutePath());

        // key = "name|postcode" to deduplicate
        Map<String, Suburb> seen = new LinkedHashMap<>();

        try (Reader reader = new FileReader(csvPath.toFile());
             var parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                try {
                    if (!"NSW".equalsIgnoreCase(record.get("state"))) continue;

                    String lat = record.get("Lat_precise").trim();
                    String lon = record.get("Long_precise").trim();
                    if (lat.isEmpty()) lat = record.get("lat").trim();
                    if (lon.isEmpty()) lon = record.get("long").trim();
                    if (lat.isEmpty() || lon.isEmpty()) continue;

                    String rawName = record.get("locality").trim();
                    if (rawName.isEmpty()) continue;

                    String rawPostcode = record.get("postcode").trim();
                    if (rawPostcode.isEmpty()) continue;

                    String name = toTitleCase(rawName);
                    String key = name.toLowerCase() + "|" + rawPostcode;
                    if (seen.containsKey(key)) continue;

                    Suburb s = new Suburb();
                    s.setName(name);
                    s.setPostcode(rawPostcode);
                    s.setLatitude(new BigDecimal(lat));
                    s.setLongitude(new BigDecimal(lon));
                    s.setRegion(RegionClassifier.classify(rawPostcode).name());
                    seen.put(key, s);
                } catch (Exception e) {
                    log.warn("Skipping malformed CSV row: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to load suburbs CSV at {}: {}", csvPath.toAbsolutePath(), e.getMessage());
        }

        List<Suburb> suburbs = List.copyOf(seen.values());
        log.info("Loaded {} NSW suburbs from CSV", suburbs.size());
        return suburbs;
    }

    private static String toTitleCase(String input) {
        String[] words = input.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
