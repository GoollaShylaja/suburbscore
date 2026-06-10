package com.suburbscore.suburb.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TransportNSWApiClient {

    private final WebClient webClient;

    public TransportNSWApiClient(@Qualifier("externalWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Value("${transport.nsw.api-key:}")
    private String apiKey;

    @Value("${transport.nsw.base-url:https://api.transport.nsw.gov.au}")
    private String baseUrl;

    @PostConstruct
    void logKeyStatus() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("TRANSPORT_NSW_API_KEY is NOT set — transport data loading will be skipped for all suburbs");
        } else {
            log.info("Transport NSW API key configured (length={}) — transport data loading enabled", apiKey.length());
        }
    }

    public TransportResult fetchTransportData(String suburbName,
                                              BigDecimal latitude,
                                              BigDecimal longitude) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Transport NSW API key not configured — skipping {}", suburbName);
            return TransportResult.empty();
        }

        try {
            double lat = latitude.doubleValue();
            double lon = longitude.doubleValue();

            List<Platform> platforms = findPlatformsByCoord(lat, lon);

            String suburbKey = suburbName.toLowerCase().replaceAll("[^a-z]", "");
            List<Platform> trainPlatforms = platforms.stream()
                    .filter(p -> isTrainPlatform(p.disassembledName()))
                    .toList();
            Platform nearestTrainPlatform = trainPlatforms.stream()
                    .filter(p -> {
                        String name = extractStationName(p.disassembledName());
                        return name != null
                                && name.toLowerCase().replaceAll("[^a-z]", "").contains(suburbKey);
                    })
                    .findFirst()
                    .or(() -> trainPlatforms.stream().findFirst())
                    .orElse(null);

            String nearestStation = nearestTrainPlatform != null
                    ? extractStationName(nearestTrainPlatform.disassembledName()) : null;
            Integer walkMins = nearestTrainPlatform != null
                    ? distanceToWalkMins(nearestTrainPlatform.properties()) : null;

            boolean hasFerry = platforms.stream()
                    .anyMatch(p -> p.disassembledName() != null && p.disassembledName().contains("Wharf"));

            int busRoutes = 0;
            List<Platform> busStops = platforms.stream()
                    .filter(p -> !isTrainPlatform(p.disassembledName())
                              && (p.disassembledName() == null || !p.disassembledName().contains("Wharf"))
                              && p.id() != null)
                    .limit(3)
                    .toList();
            for (Platform stop : busStops) {
                busRoutes = fetchBusRouteCount(stop.id());
                if (busRoutes > 0) break;
            }

            CbdCommute commute = fetchCbdCommute(lat, lon);
            Integer cbdTrain = (nearestStation != null
                    && commute.trainMins() != null
                    && commute.trainMins() <= 300)
                    ? commute.trainMins() : null;

            log.debug("Transport data for {}: station={} walkMins={} busRoutes={} ferry={} cbdTrain={}",
                    suburbName, nearestStation, walkMins, busRoutes, hasFerry, cbdTrain);

            return new TransportResult(nearestStation, walkMins, busRoutes,
                    hasFerry, cbdTrain, commute.busMins());

        } catch (Exception e) {
            log.warn("Transport NSW API failed for suburb {}: {}", suburbName, e.getMessage());
            return TransportResult.empty();
        }
    }

    // ── /coord — finds all transport platforms near a coordinate ─────────────
    // Raw URL avoids UriComponentsBuilder encoding colons in the coord parameter

    private List<Platform> findPlatformsByCoord(double lat, double lon) {
        String rawUrl = baseUrl + "/v1/tp/coord"
                + "?outputFormat=rapidJSON"
                + "&coord=" + lon + ":" + lat + ":EPSG:4326"
                + "&coordOutputFormat=EPSG:4326"
                + "&inclFilter=1"
                + "&type_1=BUS_POINT"
                + "&radius_1=1500";

        log.debug("coord URL: {}", rawUrl);

        CoordResponse response = webClient.get()
                .uri(URI.create(rawUrl))
                .header("Authorization", "apikey " + apiKey)
                .retrieve()
                .bodyToMono(CoordResponse.class)
                .block();

        if (response == null || response.locations() == null) {
            log.debug("coord returned no platforms for {},{}", lat, lon);
            return List.of();
        }

        log.debug("coord returned {} platforms for {},{}", response.locations().size(), lat, lon);
        return response.locations();
    }

    // ── /departure_mon — gets actual bus route numbers from a stop ────────────

    private int fetchBusRouteCount(String stopId) {
        String refDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rawUrl = baseUrl + "/v1/tp/departure_mon"
                + "?outputFormat=rapidJSON"
                + "&coordOutputFormat=EPSG:4326"
                + "&type_dm=stop"
                + "&name_dm=" + stopId
                + "&depArrMacro=dep"
                + "&itdDate=" + refDate
                + "&itdTime=0800"
                + "&mode=direct"
                + "&useRealtime=0"
                + "&maxStopEvents=50";

        try {
            DmResponse response = webClient.get()
                    .uri(URI.create(rawUrl))
                    .header("Authorization", "apikey " + apiKey)
                    .retrieve()
                    .bodyToMono(DmResponse.class)
                    .block();

            if (response == null || response.stopEvents() == null) return 0;

            Set<String> routes = response.stopEvents().stream()
                    .map(e -> e.transportation())
                    .filter(Objects::nonNull)
                    .map(t -> t.number() != null ? t.number() : t.name())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            log.debug("departure_mon stop {}: {} distinct routes {}", stopId, routes.size(), routes);
            return routes.size();

        } catch (Exception e) {
            log.debug("departure_mon failed for stop {}: {}", stopId, e.getMessage());
            return 0;
        }
    }

    // ── /trip — CBD commute time ──────────────────────────────────────────────

    private static final double CBD_LON = 151.2070;
    private static final double CBD_LAT = -33.8731;

    private CbdCommute fetchCbdCommute(double originLat, double originLon) {
        String refDate = nextSydneyWeekday().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String originCoord = originLon + ":" + originLat + ":EPSG:4326";
        String destCoord   = CBD_LON + ":" + CBD_LAT + ":EPSG:4326";
        String rawUrl = baseUrl + "/v1/tp/trip"
                + "?outputFormat=rapidJSON"
                + "&coordOutputFormat=EPSG:4326"
                + "&type_origin=coord"
                + "&name_origin=" + originCoord
                + "&type_destination=coord"
                + "&name_destination=" + destCoord
                + "&depArrMacro=dep"
                + "&itdDate=" + refDate
                + "&itdTime=0800"
                + "&calcNumberOfTrips=15";

        try {
            TripResponse response = webClient.get()
                    .uri(URI.create(rawUrl))
                    .header("Authorization", "apikey " + apiKey)
                    .retrieve()
                    .bodyToMono(TripResponse.class)
                    .block();

            if (response == null || response.journeys() == null || response.journeys().isEmpty()) {
                return CbdCommute.empty();
            }

            Integer trainMins = response.journeys().stream()
                    .filter(j -> j.legs() != null && hasRailLeg(j.legs()))
                    .mapToInt(j -> totalMinutes(j.legs()))
                    .filter(m -> m > 0)
                    .min()
                    .stream().boxed().findFirst().orElse(null);

            Integer busMins = response.journeys().stream()
                    .filter(j -> j.legs() != null && isBusOnly(j.legs()))
                    .mapToInt(j -> totalMinutes(j.legs()))
                    .filter(m -> m > 0)
                    .min()
                    .stream().boxed().findFirst().orElse(null);

            return new CbdCommute(trainMins, busMins);

        } catch (Exception e) {
            log.debug("Trip API failed for coord {},{}: {}", originLat, originLon, e.getMessage());
            return CbdCommute.empty();
        }
    }

    private static final int CLASS_TRAIN      = 1;
    private static final int CLASS_METRO      = 2;
    private static final int CLASS_LIGHT_RAIL = 4;
    private static final int CLASS_BUS        = 5;
    private static final int CLASS_COACH      = 7;  // NSW TrainLink intercity/regional
    private static final int CLASS_WALKING    = 99; // 99=Walking, 100=Walking(Footpath)

    private boolean hasRailLeg(List<Leg> legs) {
        return legs.stream().anyMatch(l -> {
            Integer cls = legClass(l);
            if (cls == null) return false;
            if (cls == CLASS_TRAIN || cls == CLASS_METRO || cls == CLASS_LIGHT_RAIL) return true;
            if (cls == CLASS_COACH) {
                String productName = l.transportation() != null && l.transportation().product() != null
                        ? l.transportation().product().name() : null;
                return productName != null && (
                        productName.contains("Train") ||
                        productName.contains("Rail")  ||
                        productName.contains("Link")
                );
            }
            return false;
        });
    }

    private boolean isBusOnly(List<Leg> legs) {
        return legs.stream()
                .filter(l -> legClass(l) != null && legClass(l) < CLASS_WALKING)
                .allMatch(l -> legClass(l) == CLASS_BUS);
    }

    private Integer legClass(Leg leg) {
        if (leg.transportation() == null || leg.transportation().product() == null) return null;
        return leg.transportation().product().cls();
    }

    private int totalMinutes(List<Leg> legs) {
        int seconds = legs.stream()
                .filter(l -> l.duration() != null)
                .mapToInt(Leg::duration)
                .sum();
        return seconds / 60;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final ZoneId SYDNEY_TZ = ZoneId.of("Australia/Sydney");

    private LocalDate nextSydneyWeekday() {
        LocalDate date = LocalDate.now(SYDNEY_TZ).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private boolean isTrainPlatform(String platformName) {
        return platformName != null && platformName.contains(", Platform ");
    }

    private String extractStationName(String platformName) {
        if (platformName == null) return null;
        int idx = platformName.indexOf(", Platform ");
        return idx > 0 ? platformName.substring(0, idx) : platformName;
    }

    private int distanceToWalkMins(PlatformProperties props) {
        if (props == null || props.distance() == null) return 0;
        try {
            int metres = Integer.parseInt(props.distance());
            return Math.max(1, (int) Math.round(metres / 83.0));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ── Result types ──────────────────────────────────────────────────────────

    public record TransportResult(
            String nearestTrainStation,
            Integer trainStationWalkMins,
            int numBusRoutes,
            boolean hasFerryAccess,
            Integer cbdCommuteMinsTrain,
            Integer cbdCommuteMinsBus) {

        public static TransportResult empty() {
            return new TransportResult(null, null, 0, false, null, null);
        }

        public boolean hasAnyData() {
            return nearestTrainStation != null || numBusRoutes > 0
                    || hasFerryAccess || cbdCommuteMinsTrain != null;
        }
    }

    private record CbdCommute(Integer trainMins, Integer busMins) {
        static CbdCommute empty() { return new CbdCommute(null, null); }
    }

    // ── /coord response records ───────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CoordResponse(List<Platform> locations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Platform(String id, String name, String disassembledName, String type,
                    List<Double> coord, PlatformProperties properties) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlatformProperties(String distance) {}

    // ── /departure_mon response records ──────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DmResponse(List<StopEvent> stopEvents) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StopEvent(DmTransportation transportation) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DmTransportation(String number, String name) {}

    // ── /trip response records ────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TripResponse(List<Journey> journeys) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Journey(List<Leg> legs) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Leg(Integer duration, Transportation transportation) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Transportation(Product product) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Product(@JsonProperty("class") Integer cls, String name) {}
}
