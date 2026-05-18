package com.suburbscore.suburb.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransportNSWApiClient {

    private final RestTemplate restTemplate;

    @Value("${transport.nsw.api-key}")
    private String apiKey;

    @Value("${transport.nsw.base-url:https://api.transport.nsw.gov.au}")
    private String baseUrl;

    private static final int PRODUCT_CLASS_TRAIN = 1;
    private static final int PRODUCT_CLASS_BUS   = 5;

    public TransportResult fetchTransportData(String suburbName,
                                              BigDecimal latitude,
                                              BigDecimal longitude) {
        try {
            Thread.sleep(200); // rate limit: 200ms between calls
            List<Stop> stops = findStops(suburbName);

            String nearestStation = stops.stream()
                    .filter(s -> s.productClasses() != null
                            && s.productClasses().contains(PRODUCT_CLASS_TRAIN))
                    .findFirst()
                    .map(Stop::name)
                    .orElse(null);

            int walkMins = nearestStation != null
                    ? estimateWalkMins(stops.stream()
                            .filter(s -> s.productClasses() != null
                                    && s.productClasses().contains(PRODUCT_CLASS_TRAIN))
                            .findFirst().orElse(null), latitude, longitude)
                    : 0;

            Set<String> busRoutes = stops.stream()
                    .filter(s -> s.productClasses() != null
                            && s.productClasses().contains(PRODUCT_CLASS_BUS))
                    .flatMap(s -> s.lines() != null ? s.lines().stream() : java.util.stream.Stream.empty())
                    .collect(Collectors.toSet());

            return new TransportResult(nearestStation, walkMins, busRoutes.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return TransportResult.empty();
        } catch (Exception e) {
            log.warn("Transport NSW API failed for suburb {}: {}", suburbName, e.getMessage());
            return TransportResult.empty();
        }
    }

    private List<Stop> findStops(String suburbName) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/v1/tp/stop_finder")
                .queryParam("outputFormat", "rapidJSON")
                .queryParam("type_sf", "any")
                .queryParam("name_sf", suburbName)
                .queryParam("coordOutputFormat", "EPSG:4326")
                .queryParam("TfNSWSF", true)
                .build(false)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "apikey " + apiKey);

        StopFinderResponse response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), StopFinderResponse.class
        ).getBody();

        return response != null && response.locations() != null
                ? response.locations()
                : Collections.emptyList();
    }

    private int estimateWalkMins(Stop station, BigDecimal suburbLat, BigDecimal suburbLon) {
        if (station == null || station.coord() == null || station.coord().size() < 2) return 0;
        double lat2 = station.coord().get(0);
        double lon2 = station.coord().get(1);
        double distanceKm = haversineKm(
                suburbLat.doubleValue(), suburbLon.doubleValue(), lat2, lon2);
        return (int) Math.round(distanceKm / 0.08); // avg 5km/h walk
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ── Inner records ─────────────────────────────────────────────────────────

    public record TransportResult(String nearestTrainStation,
                                  int trainStationWalkMins,
                                  int numBusRoutes) {
        public static TransportResult empty() {
            return new TransportResult(null, 0, 0);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StopFinderResponse(List<Stop> locations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Stop(String name, String type, List<Double> coord,
                List<Integer> productClasses, List<String> lines) {}
}
