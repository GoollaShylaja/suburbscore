package com.suburbscore.suburb.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenStreetMapApiClient {

    private final RestTemplate restTemplate;

    @Value("${osm.overpass.url:https://overpass-api.de/api/interpreter}")
    private String overpassUrl;

    public OsmResult fetchWalkabilityData(String suburbName) {
        try {
            Thread.sleep(1000); // rate limit: 1 second between calls

            int parks     = countFeatures(buildParkQuery(suburbName));
            int amenities = countFeatures(buildAmenityQuery(suburbName));

            return new OsmResult(parks, amenities);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return OsmResult.empty();
        } catch (Exception e) {
            log.warn("Overpass API failed for suburb {}: {}", suburbName, e.getMessage());
            return OsmResult.empty();
        }
    }

    private int countFeatures(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> request = new HttpEntity<>("data=" + query, headers);

        OverpassResponse response = restTemplate.postForObject(
                overpassUrl, request, OverpassResponse.class);

        if (response == null || response.elements() == null || response.elements().isEmpty()) {
            return 0;
        }
        // count element has a "tags" with "total" when using out count
        return response.elements().stream()
                .filter(e -> "count".equals(e.type()))
                .mapToInt(e -> e.tags() != null
                        ? Integer.parseInt(e.tags().getOrDefault("total", "0"))
                        : 0)
                .sum();
    }

    private String buildParkQuery(String suburb) {
        return """
                [out:json][timeout:20];
                area["name"="%s"]["place"~"suburb|town|village"]->.a;
                (
                  node["leisure"="park"](area.a);
                  way["leisure"="park"](area.a);
                  relation["leisure"="park"](area.a);
                );
                out count;
                """.formatted(suburb);
    }

    private String buildAmenityQuery(String suburb) {
        return """
                [out:json][timeout:20];
                area["name"="%s"]["place"~"suburb|town|village"]->.a;
                (
                  node["amenity"~"restaurant|cafe|supermarket|bank|pharmacy|hospital|gym|school|library"](area.a);
                  node["shop"~"convenience|supermarket|bakery|butcher"](area.a);
                );
                out count;
                """.formatted(suburb);
    }

    // ── Inner records ─────────────────────────────────────────────────────────

    public record OsmResult(int parksCount, int amenityCount) {
        public static OsmResult empty() { return new OsmResult(0, 0); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OverpassResponse(java.util.List<OverpassElement> elements) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OverpassElement(String type, java.util.Map<String, String> tags) {}
}
