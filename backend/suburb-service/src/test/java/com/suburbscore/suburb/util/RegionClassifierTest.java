package com.suburbscore.suburb.util;

import com.suburbscore.suburb.enums.SydneyRegion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegionClassifier")
class RegionClassifierTest {

    @ParameterizedTest(name = "postcode {0} → {1}")
    @CsvSource({
        "2000, INNER_CITY",
        "2010, INNER_CITY",
        "2020, INNER_CITY",
        "2021, EASTERN_SUBURBS",
        "2028, EASTERN_SUBURBS",
        "2036, EASTERN_SUBURBS",
        "2037, INNER_WEST",
        "2042, INNER_WEST",
        "2052, INNER_WEST",
        "2060, NORTH_SHORE",
        "2075, NORTH_SHORE",
        "2092, NORTH_SHORE",
        "2093, NORTHERN_BEACHES",
        "2100, NORTHERN_BEACHES",
        "2110, NORTHERN_BEACHES",
        "2140, WESTERN_SYDNEY",
        "2155, WESTERN_SYDNEY",
        "2170, WESTERN_SYDNEY",
        "2171, SOUTH_WESTERN_SYDNEY",
        "2190, SOUTH_WESTERN_SYDNEY",
        "2200, SOUTH_WESTERN_SYDNEY",
        "2228, SUTHERLAND",
        "2232, SUTHERLAND",
        "2234, SUTHERLAND"
    })
    @DisplayName("maps postcodes to correct regions")
    void mapsPostcodeToRegion(String postcode, SydneyRegion expected) {
        assertThat(RegionClassifier.classify(postcode)).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns WESTERN_SYDNEY for unrecognised postcode")
    void unknownPostcode_returnsDefault() {
        assertThat(RegionClassifier.classify("9999")).isEqualTo(SydneyRegion.WESTERN_SYDNEY);
    }

    @Test
    @DisplayName("returns WESTERN_SYDNEY for non-numeric input")
    void nonNumericPostcode_returnsDefault() {
        assertThat(RegionClassifier.classify("ABCD")).isEqualTo(SydneyRegion.WESTERN_SYDNEY);
    }

    @Test
    @DisplayName("handles postcode with leading/trailing whitespace")
    void postcodeWithWhitespace_classifiesCorrectly() {
        assertThat(RegionClassifier.classify(" 2042 ")).isEqualTo(SydneyRegion.INNER_WEST);
    }

    @Test
    @DisplayName("postcode outside all ranges defaults to WESTERN_SYDNEY")
    void postcodeOutsideRanges_returnsDefault() {
        assertThat(RegionClassifier.classify("2053")).isEqualTo(SydneyRegion.WESTERN_SYDNEY);
    }
}
