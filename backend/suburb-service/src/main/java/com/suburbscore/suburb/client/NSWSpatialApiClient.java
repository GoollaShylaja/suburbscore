package com.suburbscore.suburb.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suburbscore.suburb.entity.Suburb;
import com.suburbscore.suburb.util.RegionClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NSWSpatialApiClient {

    private final RestTemplate restTemplate;

    @Value("${nsw.spatial.api.url:https://portal.spatial.nsw.gov.au/server/rest/services/NSW_Administrative_Boundaries_Theme_multiCRS/FeatureServer/2/query}")
    private String apiUrl;

    private static final int PAGE_SIZE = 2000;

    public List<Suburb> fetchAllNswSuburbs() {
        List<Suburb> result = new ArrayList<>();
        int offset = 0;

        log.info("Fetching all NSW suburbs from NSW Spatial API...");

        while (true) {
            try {
                URI uri = URI.create(apiUrl
                        + "?where=state%3D2"
                        + "&outFields=suburbname%2Cpostcode"
                        + "&returnGeometry=false"
                        + "&returnCentroid=true"
                        + "&f=json"
                        + "&resultRecordCount=" + PAGE_SIZE
                        + "&resultOffset=" + offset);

                SpatialResponse response = restTemplate.getForObject(uri, SpatialResponse.class);

                if (response == null || response.features() == null || response.features().isEmpty()) {
                    break;
                }

                for (SpatialFeature feature : response.features()) {
                    Suburb suburb = toSuburb(feature);
                    if (suburb != null) result.add(suburb);
                }

                log.debug("NSW Spatial API — offset={} page={} total_so_far={}",
                        offset, response.features().size(), result.size());

                if (response.features().size() < PAGE_SIZE) break;

                offset += response.features().size();
                Thread.sleep(300);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("NSW Spatial API failed at offset {}: {}", offset, e.getMessage());
                break;
            }
        }

        log.info("NSW Spatial API — fetched {} suburbs total", result.size());
        return result;
    }

    private Suburb toSuburb(SpatialFeature feature) {
        try {
            if (feature.attributes() == null || feature.centroid() == null) return null;

            String rawName = feature.attributes().suburbName();
            if (rawName == null || rawName.isBlank()) return null;

            String rawPostcode = feature.attributes().postcode();
            if (rawPostcode == null || rawPostcode.isBlank()) return null;

            Suburb s = new Suburb();
            s.setName(toTitleCase(rawName.trim()));
            s.setPostcode(rawPostcode.trim());
            s.setLatitude(BigDecimal.valueOf(feature.centroid().y()));
            s.setLongitude(BigDecimal.valueOf(feature.centroid().x()));
            s.setRegion(RegionClassifier.classify(rawPostcode.trim()).name());
            return s;
        } catch (Exception e) {
            log.warn("Skipping invalid feature from NSW Spatial API: {}", e.getMessage());
            return null;
        }
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

    // ── API response records ──────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpatialResponse(List<SpatialFeature> features) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpatialFeature(SpatialAttributes attributes, SpatialCentroid centroid) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpatialAttributes(
            @JsonProperty("suburbname") String suburbName,
            @JsonProperty("postcode") String postcode
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpatialCentroid(double x, double y) {}
}
