package com.suburbscore.suburb.util;

import com.suburbscore.suburb.enums.SydneyRegion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegionClassifier {

    private RegionClassifier() {}

    public static SydneyRegion classify(String postcode) {
        try {
            int code = Integer.parseInt(postcode.trim());
            if (inRange(code, 2000, 2020)) return SydneyRegion.INNER_CITY;
            if (inRange(code, 2021, 2036)) return SydneyRegion.EASTERN_SUBURBS;
            // Inner West: core (Glebe/Balmain/Leichhardt) + Canada Bay (Gladesville/Concord/Rhodes)
            //             + Ashfield/Croydon + Summer Hill + Dulwich Hill/Marrickville/Wolli Creek
            if (inRange(code, 2037, 2052) || code == 2111 || code == 2130
                    || inRange(code, 2131, 2132) || inRange(code, 2137, 2138)
                    || inRange(code, 2203, 2205)) return SydneyRegion.INNER_WEST;
            // North Shore: 2110=Hunters Hill+Woolwich (Lane Cove River peninsula)
            //              2119-2120=Beecroft/Pennant Hills/Thornleigh (Hornsby LGA)
            if (inRange(code, 2060, 2092) || code == 2110
                    || inRange(code, 2119, 2120)) return SydneyRegion.NORTH_SHORE;
            if (inRange(code, 2093, 2109)) return SydneyRegion.NORTHERN_BEACHES;
            // Western Sydney: Ryde/Meadowbank, Epping/Eastwood, West Pennant Hills/Cherrybrook,
            //                 Silverwater/Newington, Burwood/Strathfield belt, Parramatta→Penrith,
            //                 Blue Mountains + Hawkesbury (outer west)
            if (inRange(code, 2112, 2118) || inRange(code, 2121, 2122) || inRange(code, 2125, 2128)
                    || inRange(code, 2133, 2136) || inRange(code, 2140, 2170)
                    || inRange(code, 2740, 2786)) return SydneyRegion.WESTERN_SYDNEY;
            // South-Western Sydney: Bankstown belt + Earlwood/Clemton Park + Campbelltown/Camden
            if (inRange(code, 2171, 2206) || inRange(code, 2555, 2574)) return SydneyRegion.SOUTH_WESTERN_SYDNEY;
            // Sutherland Shire: Georges River LGA (Bexley→Hurstville→Sylvania) + Sutherland Shire
            if (inRange(code, 2207, 2234)) return SydneyRegion.SUTHERLAND;
        } catch (NumberFormatException e) {
            log.warn("Invalid postcode '{}' — defaulting to OTHER_NSW", postcode);
        }
        return SydneyRegion.OTHER_NSW;
    }

    private static boolean inRange(int code, int lo, int hi) {
        return code >= lo && code <= hi;
    }
}
