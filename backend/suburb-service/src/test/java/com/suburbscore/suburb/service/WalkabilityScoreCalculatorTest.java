package com.suburbscore.suburb.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WalkabilityScoreCalculatorTest {

    @Test
    void score_isZero_whenBothCountsAreZero() {
        assertThat(score(0, 0)).isEqualByComparingTo("0.0");
    }

    @Test
    void score_capsAmenityContributionAt70() {
        // 1000 amenities → amenity score = 70 (capped), parks = 0
        assertThat(score(0, 1000)).isEqualByComparingTo("70.0");
    }

    @Test
    void score_capsParkContributionAt30() {
        // 20 parks → park score = 30 (capped), amenities = 0
        assertThat(score(20, 0)).isEqualByComparingTo("30.0");
    }

    @Test
    void score_capsAt100_whenBothMaxed() {
        assertThat(score(20, 1000)).isEqualByComparingTo("100.0");
    }

    @Test
    void score_innerCitySuburb_isHigh() {
        // Surry Hills style: ~12 parks, ~800 amenities → 36 + 70 → capped at 100
        assertThat(score(12, 800)).isEqualByComparingTo("100.0");
    }

    @Test
    void score_midRingSuburb_isMidRange() {
        // e.g. Strathfield: ~5 parks, ~200 amenities → 15 + 20 = 35
        assertThat(score(5, 200)).isEqualByComparingTo("35.0");
    }

    @Test
    void score_outerSuburb_isLow() {
        // e.g. Kellyville: ~3 parks, ~60 amenities → 9 + 6 = 15
        assertThat(score(3, 60)).isEqualByComparingTo("15.0");
    }

    @Test
    void score_ruralTown_isVeryLow() {
        // ~1 park, ~8 amenities → 3 + 0.8 = 3.8
        assertThat(score(1, 8)).isEqualByComparingTo("3.8");
    }

    @Test
    void score_hasOneDecimalPlace() {
        BigDecimal result = score(2, 15);
        assertThat(result.scale()).isEqualTo(1);
    }

    private static BigDecimal score(int parks, int amenities) {
        return WalkabilityDataLoaderService.calculateWalkabilityScore(parks, amenities);
    }
}
