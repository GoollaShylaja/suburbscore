package com.suburbscore.suburb.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OpenStreetMapApiClientTest {

    // Combined response: first count = parks, second count = amenities
    private static final String COMBINED_OK = """
            {"elements":[
              {"type":"count","tags":{"nodes":"1","ways":"4","relations":"0","total":"5"}},
              {"type":"count","tags":{"nodes":"12","ways":"0","relations":"0","total":"12"}}
            ]}""";

    private static final String COMBINED_ZEROS = """
            {"elements":[
              {"type":"count","tags":{"total":"0"}},
              {"type":"count","tags":{"total":"0"}}
            ]}""";

    private MockWebServer mockServer;
    private OpenStreetMapApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .build();

        client = new OpenStreetMapApiClient(webClient);
        ReflectionTestUtils.setField(client, "overpassUrl",
                mockServer.url("/api/interpreter").toString());
        ReflectionTestUtils.setField(client, "requestIntervalMs", 0L);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void fetchWalkabilityData_returnsBothCounts_whenOverpassResponds() throws InterruptedException {
        mockServer.enqueue(new MockResponse()
                .setBody(COMBINED_OK)
                .addHeader("Content-Type", "application/json"));

        OpenStreetMapApiClient.OsmResult result = client.fetchWalkabilityData(
                "Newtown", new BigDecimal("-33.8980"), new BigDecimal("151.1794"));

        assertThat(result.parksCount()).isEqualTo(5);
        assertThat(result.amenityCount()).isEqualTo(12);
        assertThat(mockServer.getRequestCount()).isEqualTo(1); // single combined request
    }

    @Test
    void fetchWalkabilityData_sendsOneRequest_perSuburb() throws InterruptedException {
        mockServer.enqueue(new MockResponse()
                .setBody(COMBINED_OK)
                .addHeader("Content-Type", "application/json"));

        client.fetchWalkabilityData(
                "Surry Hills", new BigDecimal("-33.8864"), new BigDecimal("151.2094"));

        // Exactly 1 POST issued — combined query, not 2 separate ones
        assertThat(mockServer.getRequestCount()).isEqualTo(1);

        RecordedRequest req = mockServer.takeRequest();
        assertThat(req.getMethod()).isEqualTo("POST");
        assertThat(req.getBody().readUtf8()).contains("data=");
    }

    @Test
    void fetchWalkabilityData_returnsZeros_whenCountsAreZero() {
        mockServer.enqueue(new MockResponse()
                .setBody(COMBINED_ZEROS)
                .addHeader("Content-Type", "application/json"));

        OpenStreetMapApiClient.OsmResult result = client.fetchWalkabilityData(
                "Outback", new BigDecimal("-30.0"), new BigDecimal("145.0"));

        assertThat(result.parksCount()).isZero();
        assertThat(result.amenityCount()).isZero();
    }

    @Test
    void fetchWalkabilityData_usesNameFallback_whenNoCoordinates() {
        mockServer.enqueue(new MockResponse()
                .setBody(COMBINED_OK)
                .addHeader("Content-Type", "application/json"));

        OpenStreetMapApiClient.OsmResult result = client.fetchWalkabilityData("Glebe", null, null);

        assertThat(result.parksCount()).isEqualTo(5);
        assertThat(result.amenityCount()).isEqualTo(12);
        assertThat(mockServer.getRequestCount()).isEqualTo(1);
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test
    void fetchWalkabilityData_returnsEmpty_whenOverpassReturns500() {
        // 500 is not retryable — fails immediately, no retry
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        OpenStreetMapApiClient.OsmResult result = client.fetchWalkabilityData(
                "Broken", new BigDecimal("-33.8980"), new BigDecimal("151.1794"));

        assertThat(result).isEqualTo(OpenStreetMapApiClient.OsmResult.empty());
        assertThat(mockServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void fetchWalkabilityData_retriesOn429_thenSucceeds() {
        // First request 429, second request succeeds
        mockServer.enqueue(new MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "1")); // 1s so test doesn't take forever
        mockServer.enqueue(new MockResponse()
                .setBody(COMBINED_OK)
                .addHeader("Content-Type", "application/json"));

        OpenStreetMapApiClient.OsmResult result = client.fetchWalkabilityData(
                "Newtown", new BigDecimal("-33.8980"), new BigDecimal("151.1794"));

        assertThat(result.parksCount()).isEqualTo(5);
        assertThat(result.amenityCount()).isEqualTo(12);
        assertThat(mockServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void fetchWalkabilityData_exhaustsRetries_returnsEmpty() {
        // All 4 attempts (1 initial + 3 retries) return 429 with Retry-After: 1
        for (int i = 0; i < 4; i++) {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(429)
                    .addHeader("Retry-After", "1"));
        }

        OpenStreetMapApiClient.OsmResult result = client.fetchWalkabilityData(
                "RateLimited", new BigDecimal("-33.8980"), new BigDecimal("151.1794"));

        assertThat(result).isEqualTo(OpenStreetMapApiClient.OsmResult.empty());
        assertThat(mockServer.getRequestCount()).isEqualTo(4);
    }

    @Test
    void fetchWalkabilityData_retriesOn504_thenSucceeds() {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(504)
                .addHeader("Retry-After", "1"));
        mockServer.enqueue(new MockResponse()
                .setBody(COMBINED_OK)
                .addHeader("Content-Type", "application/json"));

        OpenStreetMapApiClient.OsmResult result = client.fetchWalkabilityData(
                "Timeout", new BigDecimal("-33.8980"), new BigDecimal("151.1794"));

        assertThat(result.parksCount()).isEqualTo(5);
        assertThat(mockServer.getRequestCount()).isEqualTo(2);
    }

    // ── Record tests ──────────────────────────────────────────────────────────

    @Test
    void osmResult_empty_returnsZeros() {
        OpenStreetMapApiClient.OsmResult empty = OpenStreetMapApiClient.OsmResult.empty();
        assertThat(empty.parksCount()).isZero();
        assertThat(empty.amenityCount()).isZero();
    }
}
