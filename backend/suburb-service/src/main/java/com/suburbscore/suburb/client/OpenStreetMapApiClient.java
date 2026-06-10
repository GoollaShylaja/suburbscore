package com.suburbscore.suburb.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class OpenStreetMapApiClient {

    // ~2.5 km radius bounding box around suburb centre
    private static final double LAT_DELTA = 0.022;
    private static final double LON_DELTA = 0.027;

    private static final Set<Integer> RETRYABLE = Set.of(429, 503, 504);
    private static final int MAX_RETRIES = 3;

    private final WebClient webClient;

    @Value("${osm.overpass.url:https://overpass-api.de/api/interpreter}")
    private String overpassUrl;

    // Sleep between suburbs — public Overpass allows ~1 req/s per IP; 3 s is conservative
    @Value("${osm.request-interval-ms:3000}")
    private long requestIntervalMs;

    public OpenStreetMapApiClient(@Qualifier("osmWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetches park AND amenity counts in a single Overpass request.
     * The combined query issues `.parks out count` then `.amenities out count`,
     * returning two count elements — first is parks, second is amenities.
     * Retries 429 / 503 / 504 up to 3 times, honouring the Retry-After header.
     */
    public OsmResult fetchWalkabilityData(String suburbName, BigDecimal lat, BigDecimal lon) {
        try {
            Thread.sleep(requestIntervalMs);

            boolean hasCoords = lat != null && lon != null;
            String query = hasCoords
                    ? buildCombinedBboxQuery(lat, lon)
                    : buildCombinedNameQuery(suburbName);

            return executeQuery(query, suburbName);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return OsmResult.empty();
        } catch (Exception e) {
            log.warn("Overpass failed for suburb {} after all retries: {}", suburbName, e.getMessage());
            return OsmResult.empty();
        }
    }

    private OsmResult executeQuery(String query, String suburbName) {
        String encoded = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        OverpassResponse response = webClient.post()
                .uri(URI.create(overpassUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(encoded)
                .retrieve()
                .bodyToMono(OverpassResponse.class)
                .retryWhen(buildRetry())
                .block();

        if (response == null || response.elements() == null) {
            return OsmResult.empty();
        }

        List<OverpassElement> counts = response.elements().stream()
                .filter(e -> "count".equals(e.type()))
                .toList();

        int parks     = parseTotal(counts, 0);
        int amenities = parseTotal(counts, 1);
        return new OsmResult(parks, amenities);
    }

    private static int parseTotal(List<OverpassElement> counts, int index) {
        if (index >= counts.size()) return 0;
        OverpassElement el = counts.get(index);
        if (el.tags() == null) return 0;
        try {
            return Integer.parseInt(el.tags().getOrDefault("total", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Retry strategy: honours the Retry-After header Overpass sends with 429.
     * Falls back to exponential backoff (30 s → 60 s → 120 s) for 503/504.
     */
    private Retry buildRetry() {
        return Retry.from(companion -> companion.flatMap(rs -> {
            if (rs.totalRetries() >= MAX_RETRIES) {
                return Mono.error(rs.failure());
            }
            if (!(rs.failure() instanceof WebClientResponseException ex)
                    || !RETRYABLE.contains(ex.getStatusCode().value())) {
                return Mono.error(rs.failure());
            }

            long delaySec = retryAfterSeconds(ex, 30L * (1L << rs.totalRetries()));
            log.warn("Overpass {} (attempt {}/{}), waiting {} s — suburb processing paused",
                    ex.getStatusCode().value(), rs.totalRetries() + 1, MAX_RETRIES, delaySec);
            return Mono.delay(Duration.ofSeconds(delaySec));
        }));
    }

    private static long retryAfterSeconds(WebClientResponseException ex, long fallback) {
        String header = ex.getHeaders().getFirst("Retry-After");
        if (header != null) {
            try {
                return Math.max(1L, Long.parseLong(header.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return Math.min(fallback, 120L);
    }

    // ── Combined queries — 1 HTTP call returns both park and amenity counts ──

    private String buildCombinedBboxQuery(BigDecimal lat, BigDecimal lon) {
        double s = lat.doubleValue() - LAT_DELTA;
        double n = lat.doubleValue() + LAT_DELTA;
        double w = lon.doubleValue() - LON_DELTA;
        double e = lon.doubleValue() + LON_DELTA;
        return """
                [out:json][timeout:45][bbox:%f,%f,%f,%f];
                (
                  node["leisure"="park"];
                  way["leisure"="park"];
                  relation["leisure"="park"];
                )->.parks;
                (
                  node["amenity"~"restaurant|cafe|supermarket|bank|pharmacy|hospital|gym|library"];
                  node["shop"~"convenience|supermarket|bakery|butcher"];
                )->.amenities;
                .parks out count;
                .amenities out count;
                """.formatted(s, w, n, e);
    }

    private String buildCombinedNameQuery(String suburb) {
        return """
                [out:json][timeout:45];
                area["name"="%s"]["place"~"suburb|town|village"]->.a;
                (
                  node["leisure"="park"](area.a);
                  way["leisure"="park"](area.a);
                  relation["leisure"="park"](area.a);
                )->.parks;
                (
                  node["amenity"~"restaurant|cafe|supermarket|bank|pharmacy|hospital|gym|library"](area.a);
                  node["shop"~"convenience|supermarket|bakery|butcher"](area.a);
                )->.amenities;
                .parks out count;
                .amenities out count;
                """.formatted(suburb);
    }

    // ── Inner records ─────────────────────────────────────────────────────────

    public record OsmResult(int parksCount, int amenityCount) {
        public static OsmResult empty() { return new OsmResult(0, 0); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OverpassResponse(List<OverpassElement> elements) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OverpassElement(String type, java.util.Map<String, String> tags) {}
}
