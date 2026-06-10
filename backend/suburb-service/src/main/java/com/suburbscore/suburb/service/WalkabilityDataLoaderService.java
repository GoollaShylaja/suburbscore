package com.suburbscore.suburb.service;

import com.suburbscore.suburb.client.OpenStreetMapApiClient;
import com.suburbscore.suburb.entity.Suburb;
import com.suburbscore.suburb.entity.SuburbStats;
import com.suburbscore.suburb.repository.SuburbRepository;
import com.suburbscore.suburb.repository.SuburbStatsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class WalkabilityDataLoaderService {

    // Overpass public API: max 2 concurrent queries per IP.
    // Stagger the second worker so requests interleave rather than collide.
    private static final int  WORKER_COUNT    = 2;
    private static final long STAGGER_MS      = 1_500;

    private final SuburbRepository      suburbRepository;
    private final SuburbStatsRepository suburbStatsRepository;
    private final OpenStreetMapApiClient osmApiClient;
    private final Executor              asyncExecutor;

    public WalkabilityDataLoaderService(
            SuburbRepository suburbRepository,
            SuburbStatsRepository suburbStatsRepository,
            OpenStreetMapApiClient osmApiClient,
            @Qualifier("asyncExecutor") Executor asyncExecutor) {
        this.suburbRepository      = suburbRepository;
        this.suburbStatsRepository = suburbStatsRepository;
        this.osmApiClient          = osmApiClient;
        this.asyncExecutor         = asyncExecutor;
    }

    @Async("asyncExecutor")
    public void loadForAllSuburbsAsync() {
        log.info("Starting walkability data load via Overpass API ({} parallel workers)...", WORKER_COUNT);
        List<Suburb> suburbs = suburbRepository.findAll();
        int updated = runParallel(suburbs);
        log.info("Walkability data load complete — {} / {} suburbs updated", updated, suburbs.size());
    }

    @Async("asyncExecutor")
    public void reloadAllAsync() {
        log.info("Reloading walkability data for all suburbs ({} parallel workers)...", WORKER_COUNT);
        List<Suburb> suburbs = suburbRepository.findAll();
        int updated = runParallel(suburbs);
        log.info("Walkability data reload complete — {} / {} suburbs updated", updated, suburbs.size());
    }

    // ── Parallel execution ────────────────────────────────────────────────────

    private int runParallel(List<Suburb> suburbs) {
        List<List<Suburb>> batches = partition(suburbs, WORKER_COUNT);
        AtomicInteger total = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < batches.size(); i++) {
            final List<Suburb> batch  = batches.get(i);
            final int          worker = i + 1;
            final long         delay  = i * STAGGER_MS;

            futures.add(CompletableFuture.runAsync(() -> {
                if (delay > 0) {
                    try { Thread.sleep(delay); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
                total.addAndGet(processBatch(batch, worker));
            }, asyncExecutor));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Walkability parallel load error: {}", e.getMessage());
        }
        return total.get();
    }

    // ── Per-batch sequential processing ──────────────────────────────────────

    public int processBatch(List<Suburb> suburbs, int workerNum) {
        int updated = 0;
        for (Suburb suburb : suburbs) {
            try {
                OpenStreetMapApiClient.OsmResult result =
                        osmApiClient.fetchWalkabilityData(
                                suburb.getSuburbName(),
                                suburb.getLatitude(),
                                suburb.getLongitude());

                if (result.parksCount() == 0 && result.amenityCount() == 0) continue;

                SuburbStats stats = suburbStatsRepository.findBySuburbId(suburb.getId())
                        .orElseGet(() -> {
                            SuburbStats s = new SuburbStats();
                            s.setSuburb(suburb);
                            return s;
                        });
                stats.setParksCount(result.parksCount());
                stats.setWalkabilityAmenityCount(result.amenityCount());
                stats.setWalkabilityScore(calculateWalkabilityScore(result.parksCount(), result.amenityCount()));
                suburbStatsRepository.save(stats);
                updated++;

                if (updated % 50 == 0) {
                    log.info("[Worker {}] Walkability progress: {} suburbs updated", workerNum, updated);
                }
            } catch (Exception e) {
                log.warn("[Worker {}] OSM fetch failed for {}: {}",
                        workerNum, suburb.getSuburbName(), e.getMessage());
            }
        }
        log.info("[Worker {}] Batch complete — {} suburbs updated out of {}", workerNum, updated, suburbs.size());
        return updated;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static <T> List<List<T>> partition(List<T> list, int count) {
        List<List<T>> result = new ArrayList<>();
        int size = (list.size() + count - 1) / count;
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    /**
     * Scores walkability 0–100 from raw OSM counts.
     *   Amenity score : min(70, amenityCount / 10)   — 700+ amenities = inner-city walkable
     *   Park score    : min(30, parksCount  * 3)     — 10+ parks = max park contribution
     */
    static BigDecimal calculateWalkabilityScore(int parksCount, int amenityCount) {
        double amenityScore = Math.min(70.0, amenityCount / 10.0);
        double parkScore    = Math.min(30.0, parksCount   * 3.0);
        double total        = Math.min(100.0, amenityScore + parkScore);
        return BigDecimal.valueOf(total).setScale(1, RoundingMode.HALF_UP);
    }
}
