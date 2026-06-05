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
        "2060, NORTH_SHORE_RYDE",
        "2075, NORTH_SHORE_RYDE",
        "2092, NORTH_SHORE_RYDE",
        "2110, NORTH_SHORE_RYDE",
        "2112, NORTH_SHORE_RYDE",
        "2093, NORTHERN_BEACHES",
        "2100, NORTHERN_BEACHES",
        "2118, THE_HILLS_DISTRICT",
        "2153, THE_HILLS_DISTRICT",
        "2155, THE_HILLS_DISTRICT",
        "2140, WESTERN_SYDNEY",
        "2150, WESTERN_SYDNEY",
        "2160, WESTERN_SYDNEY",
        "2161, WESTERN_SYDNEY",
        "2162, SOUTH_WESTERN_SYDNEY",
        "2170, SOUTH_WESTERN_SYDNEY",
        "2171, SOUTH_WESTERN_SYDNEY",
        "2190, SOUTH_WESTERN_SYDNEY",
        "2193, INNER_WEST",
        "2200, SOUTH_WESTERN_SYDNEY",
        "2201, SUTHERLAND_ST_GEORGE",
        "2203, INNER_WEST",
        "2205, SUTHERLAND_ST_GEORGE",
        "2206, INNER_WEST",
        "2207, SUTHERLAND_ST_GEORGE",
        "2228, SUTHERLAND_ST_GEORGE",
        "2234, SUTHERLAND_ST_GEORGE",
        "2555, MACARTHUR",
        "2558, MACARTHUR",
        "2560, MACARTHUR",
        "2570, MACARTHUR",
        "2753, HAWKESBURY",
        "2775, HAWKESBURY",
        "2773, BLUE_MOUNTAINS",
        "2780, BLUE_MOUNTAINS",
        "2250, CENTRAL_COAST",
        "2260, CENTRAL_COAST",
        "2264, HUNTER_NEWCASTLE",
        "2300, HUNTER_NEWCASTLE",
        "2330, HUNTER_NEWCASTLE",
        "2331, NEW_ENGLAND",
        "2340, NEW_ENGLAND",
        "2350, NEW_ENGLAND",
        "2419, NEW_ENGLAND",
        "2420, MID_NORTH_COAST",
        "2440, MID_NORTH_COAST",
        "2450, MID_NORTH_COAST",
        "2459, MID_NORTH_COAST",
        "2460, NORTH_COAST",
        "2480, NORTH_COAST",
        "2490, NORTH_COAST",
        "2500, ILLAWARRA",
        "2520, ILLAWARRA",
        "2532, ILLAWARRA",
        "2533, SOUTH_COAST",
        "2540, SOUTH_COAST",
        "2550, SOUTH_COAST",
        "2580, SOUTHERN_HIGHLANDS_GOULBURN",
        "2619, SNOWY_MONARO",
        "2630, SNOWY_MONARO",
        "2639, SNOWY_MONARO",
        "2640, RIVERINA",
        "2650, RIVERINA",
        "2700, RIVERINA",
        "2739, RIVERINA",
        "2800, CENTRAL_WEST",
        "2850, CENTRAL_WEST"
    })
    @DisplayName("maps postcodes to correct regions")
    void mapsPostcodeToRegion(String postcode, SydneyRegion expected) {
        assertThat(RegionClassifier.classify(postcode)).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns OTHER_NSW for unrecognised postcode")
    void unknownPostcode_returnsDefault() {
        assertThat(RegionClassifier.classify("9999")).isEqualTo(SydneyRegion.OTHER_NSW);
    }

    @Test
    @DisplayName("ACT postcodes with NSW suburbs correctly classified by region")
    void actPostcodes_nswSuburbsClassifiedCorrectly() {
        assertThat(RegionClassifier.classify("2601")).isEqualTo(SydneyRegion.OTHER_NSW);             // genuine ACT
        assertThat(RegionClassifier.classify("2611")).isEqualTo(SydneyRegion.SNOWY_MONARO);          // Bimberi, Cooleman, Brindabella
        assertThat(RegionClassifier.classify("2618")).isEqualTo(SydneyRegion.SOUTHERN_HIGHLANDS_GOULBURN); // Wallaroo, Springrange
    }

    @Test
    @DisplayName("Uriarra (2611) overridden to SOUTHERN_HIGHLANDS_GOULBURN via name lookup")
    void uriarraNameOverride() {
        assertThat(RegionClassifier.classify("Uriarra", "2611")).isEqualTo(SydneyRegion.SOUTHERN_HIGHLANDS_GOULBURN);
        assertThat(RegionClassifier.classify("Bimberi",     "2611")).isEqualTo(SydneyRegion.SNOWY_MONARO);
        assertThat(RegionClassifier.classify("Brindabella", "2611")).isEqualTo(SydneyRegion.SNOWY_MONARO);
    }

    @Test
    @DisplayName("Victorian postcodes used by Murray/Snowy border towns correctly classified")
    void victorianPostcodes_nswBorderTownsClassifiedCorrectly() {
        assertThat(RegionClassifier.classify("3644")).isEqualTo(SydneyRegion.RIVERINA);      // Barooga, Lalalty
        assertThat(RegionClassifier.classify("3691")).isEqualTo(SydneyRegion.RIVERINA);      // Lake Hume Village
        assertThat(RegionClassifier.classify("3707")).isEqualTo(SydneyRegion.SNOWY_MONARO); // Bringenbrong
    }

    @Test
    @DisplayName("Queensland postcodes used by Tenterfield border towns correctly classified")
    void queenslandPostcodes_nswBorderTownsClassifiedCorrectly() {
        assertThat(RegionClassifier.classify("4375")).isEqualTo(SydneyRegion.NEW_ENGLAND); // Cottonvale
        assertThat(RegionClassifier.classify("4377")).isEqualTo(SydneyRegion.NEW_ENGLAND); // Maryland/Tenterfield
        assertThat(RegionClassifier.classify("4380")).isEqualTo(SydneyRegion.NEW_ENGLAND); // Ruby Creek, Mingoola
        assertThat(RegionClassifier.classify("4383")).isEqualTo(SydneyRegion.NEW_ENGLAND); // Jennings
        assertThat(RegionClassifier.classify("4385")).isEqualTo(SydneyRegion.NEW_ENGLAND); // Camp Creek, Texas
    }

    @Test
    @DisplayName("Maryland 2287 (Newcastle) and Maryland 4377 (Tenterfield) resolve to different regions")
    void maryland_disambiguatedByPostcode() {
        assertThat(RegionClassifier.classify("Maryland", "2287")).isEqualTo(SydneyRegion.HUNTER_NEWCASTLE);
        assertThat(RegionClassifier.classify("Maryland", "4377")).isEqualTo(SydneyRegion.NEW_ENGLAND);
    }

    @Test
    @DisplayName("returns OTHER_NSW for non-numeric input")
    void nonNumericPostcode_returnsDefault() {
        assertThat(RegionClassifier.classify("ABCD")).isEqualTo(SydneyRegion.OTHER_NSW);
    }

    @Test
    @DisplayName("handles postcode with leading/trailing whitespace")
    void postcodeWithWhitespace_classifiesCorrectly() {
        assertThat(RegionClassifier.classify(" 2042 ")).isEqualTo(SydneyRegion.INNER_WEST);
    }

    @Test
    @DisplayName("postcode outside all ranges defaults to OTHER_NSW")
    void postcodeOutsideRanges_returnsDefault() {
        assertThat(RegionClassifier.classify("2053")).isEqualTo(SydneyRegion.OTHER_NSW);
    }
}
