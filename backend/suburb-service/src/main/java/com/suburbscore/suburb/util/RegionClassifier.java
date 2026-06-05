package com.suburbscore.suburb.util;

import com.suburbscore.suburb.enums.SydneyRegion;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class RegionClassifier {

    private RegionClassifier() {}

    // Key = upper bound (end) of each range — ceilingEntry(code) finds the tightest enclosing range
    private static final TreeMap<Integer, RangeDefinition> RANGE_MAP = new TreeMap<>();

    // Name-based overrides for suburbs whose postcode is shared across two NSW regions.
    // Key: lowercase suburb name → postcode → region. Checked before the postcode range lookup.
    private static final Map<String, Map<String, SydneyRegion>> NAME_OVERRIDES = Map.of(
        // Postcode 2611 majority → SNOWY_MONARO, but Uriarra is Yass Valley Council
        "uriarra", Map.of("2611", SydneyRegion.SOUTHERN_HIGHLANDS_GOULBURN)
    );

    private static class RangeDefinition {
        final int start;
        final SydneyRegion region;

        RangeDefinition(int start, SydneyRegion region) {
            this.start = start;
            this.region = region;
        }
    }

    static {
        // INNER_CITY: Sydney CBD, Pyrmont, Surry Hills, Darlinghurst, Redfern, Waterloo
        add(2000, 2020, SydneyRegion.INNER_CITY);

        // EASTERN_SUBURBS: Paddington, Bondi, Coogee, Randwick, Maroubra, Double Bay, Vaucluse
        add(2021, 2036, SydneyRegion.EASTERN_SUBURBS);

        // INNER_WEST: Glebe, Balmain, Leichhardt, Newtown, Marrickville, Ashfield/Croydon, Canada Bay
        add(2037, 2052, SydneyRegion.INNER_WEST);
        add(2130, 2132, SydneyRegion.INNER_WEST);
        add(2137, 2138, SydneyRegion.INNER_WEST);
        add(2193, 2193, SydneyRegion.INNER_WEST);   // Hurlstone Park, Ashbury
        add(2203, 2204, SydneyRegion.INNER_WEST);   // Dulwich Hill, Marrickville South
        add(2206, 2206, SydneyRegion.INNER_WEST);   // Earlwood, Clemton Park

        // NORTH_SHORE_RYDE: Lower/Upper North Shore, Hunters Hill, Ryde LGA, Epping, Marsfield
        add(2060, 2092, SydneyRegion.NORTH_SHORE_RYDE);
        add(2110, 2111, SydneyRegion.NORTH_SHORE_RYDE);   // Hunters Hill, Woolwich
        add(2112, 2117, SydneyRegion.NORTH_SHORE_RYDE);   // Ryde, Meadowbank, Eastwood, West Ryde
        add(2121, 2122, SydneyRegion.NORTH_SHORE_RYDE);   // Epping, Marsfield

        // NORTHERN_BEACHES: Manly, Dee Why, Mona Vale, Palm Beach
        add(2093, 2109, SydneyRegion.NORTHERN_BEACHES);

        // THE_HILLS_DISTRICT: Carlingford, Beecroft/Pennant Hills, West Pennant Hills,
        //                     Baulkham Hills, Castle Hill, Kellyville, Dural
        add(2118, 2120, SydneyRegion.THE_HILLS_DISTRICT);   // Carlingford, Beecroft, Pennant Hills
        add(2125, 2126, SydneyRegion.THE_HILLS_DISTRICT);   // West Pennant Hills, Cherrybrook
        add(2153, 2159, SydneyRegion.THE_HILLS_DISTRICT);   // Baulkham Hills, Castle Hill, Norwest, Dural

        // WESTERN_SYDNEY: Silverwater/Olympic Park, Parramatta, Blacktown, Penrith, Mt Druitt,
        //                 Riverstone, Marsden Park (2765 Box Hill absorbed here — shared postcode)
        add(2123, 2124, SydneyRegion.WESTERN_SYDNEY);
        add(2127, 2128, SydneyRegion.WESTERN_SYDNEY);
        add(2133, 2136, SydneyRegion.WESTERN_SYDNEY);   // Strathfield, Burwood belt
        add(2140, 2152, SydneyRegion.WESTERN_SYDNEY);   // Parramatta, Northmead, Homebush
        add(2160, 2161, SydneyRegion.WESTERN_SYDNEY);   // Merrylands, Guildford
        add(2740, 2752, SydneyRegion.WESTERN_SYDNEY);   // Penrith, Emu Plains, Jamisontown
        add(2759, 2771, SydneyRegion.WESTERN_SYDNEY);   // Blacktown, Riverstone, Marsden Park

        // SOUTH_WESTERN_SYDNEY: Fairfield, Liverpool, Bankstown, Campsie
        add(2162, 2169, SydneyRegion.SOUTH_WESTERN_SYDNEY);   // Fairfield, Chester Hill, Villawood
        add(2170, 2170, SydneyRegion.SOUTH_WESTERN_SYDNEY);   // Liverpool, Moorebank, Chipping Norton
        add(2171, 2192, SydneyRegion.SOUTH_WESTERN_SYDNEY);   // Green Valley, Bankstown, Belmore
        add(2194, 2200, SydneyRegion.SOUTH_WESTERN_SYDNEY);   // Campsie, Bankstown aerodrome fringe

        // SUTHERLAND_ST_GEORGE: Arncliffe/Wolli Creek fringe, Georges River LGA, Sutherland Shire
        add(2201, 2202, SydneyRegion.SUTHERLAND_ST_GEORGE);
        add(2205, 2205, SydneyRegion.SUTHERLAND_ST_GEORGE);   // Arncliffe, Wolli Creek
        add(2207, 2234, SydneyRegion.SUTHERLAND_ST_GEORGE);   // Hurstville, Kogarah to Cronulla

        // MACARTHUR: Ingleburn, Campbelltown, Camden, Narellan
        add(2555, 2574, SydneyRegion.MACARTHUR);

        // HAWKESBURY: Richmond, Windsor, Wilberforce, Pitt Town, Kurrajong, Kurmond
        add(2753, 2758, SydneyRegion.HAWKESBURY);
        add(2775, 2775, SydneyRegion.HAWKESBURY);   // Kurmond (Hawkesbury LGA)

        // BLUE_MOUNTAINS: Glenbrook to Mount Victoria (2775 Kurmond excluded — Hawkesbury)
        add(2773, 2774, SydneyRegion.BLUE_MOUNTAINS);
        add(2776, 2786, SydneyRegion.BLUE_MOUNTAINS);

        // CENTRAL_COAST: Gosford and Wyong LGAs
        add(2250, 2263, SydneyRegion.CENTRAL_COAST);

        // CROSS-BORDER EXCEPTIONS
        // NSW suburbs that use ACT, Victorian, or Queensland postal routing.
        // Uriarra (2611) is the only split: classified via NAME_OVERRIDES → SOUTHERN_HIGHLANDS_GOULBURN.
        add(2611, 2611, SydneyRegion.SNOWY_MONARO);             // Bimberi, Cooleman, Brindabella (Snowy Valleys Council)
        add(2618, 2618, SydneyRegion.SOUTHERN_HIGHLANDS_GOULBURN); // Wallaroo, Springrange (Yass Valley Shire)
        add(3644, 3644, SydneyRegion.RIVERINA);                 // Barooga, Lalalty (Berrigan Shire)
        add(3691, 3691, SydneyRegion.RIVERINA);                 // Lake Hume Village (Albury City Council)
        add(3707, 3707, SydneyRegion.SNOWY_MONARO);             // Bringenbrong (Snowy Valleys Council)
        add(4375, 4375, SydneyRegion.NEW_ENGLAND);              // Cottonvale (Tenterfield Shire)
        add(4377, 4377, SydneyRegion.NEW_ENGLAND);              // Maryland/Tenterfield (≠ Maryland 2287, Newcastle)
        add(4380, 4380, SydneyRegion.NEW_ENGLAND);              // Ruby Creek, Amosfield, Mingoola, Undercliffe
        add(4383, 4383, SydneyRegion.NEW_ENGLAND);              // Jennings (Tenterfield Shire)
        add(4385, 4385, SydneyRegion.NEW_ENGLAND);              // Camp Creek, Texas (Goondiwindi border)

        // NSW REGIONS

        // HUNTER_NEWCASTLE: Lake Macquarie, Newcastle, Maitland, Cessnock, Singleton
        add(2264, 2330, SydneyRegion.HUNTER_NEWCASTLE);

        // NEW_ENGLAND: Upper Hunter, Tamworth, Armidale, Inverell, Narrabri, North West NSW
        add(2331, 2419, SydneyRegion.NEW_ENGLAND);

        // MID_NORTH_COAST: Taree, Port Macquarie, Kempsey, Coffs Harbour, Nambucca
        add(2420, 2459, SydneyRegion.MID_NORTH_COAST);

        // NORTH_COAST: Grafton, Lismore, Byron Bay, Ballina, Tweed Heads
        add(2460, 2499, SydneyRegion.NORTH_COAST);

        // ILLAWARRA: Wollongong, Shellharbour, Kiama fringe
        add(2500, 2532, SydneyRegion.ILLAWARRA);

        // SOUTH_COAST: Kiama, Nowra/Shoalhaven, Ulladulla, Batemans Bay, Bega, Eden
        // Ends at 2554 — MACARTHUR starts at 2555
        add(2533, 2554, SydneyRegion.SOUTH_COAST);

        // SOUTHERN_HIGHLANDS_GOULBURN: Bowral, Moss Vale, Bundanoon, Goulburn, Yass
        // Ends at 2599 — 2600-2618 are ACT postcodes → OTHER_NSW (intentional gap)
        add(2575, 2599, SydneyRegion.SOUTHERN_HIGHLANDS_GOULBURN);

        // SNOWY_MONARO: Queanbeyan, Cooma, Jindabyne, Snowy Mountains
        // Starts at 2619 — skips ACT postcodes 2600-2618
        add(2619, 2639, SydneyRegion.SNOWY_MONARO);

        // RIVERINA: Albury, Wagga Wagga, Griffith, Narrandera, Leeton, Tumut
        // Ends at 2739 — WESTERN_SYDNEY (Penrith) starts at 2740
        add(2640, 2739, SydneyRegion.RIVERINA);

        // CENTRAL_WEST: Lithgow fringe, Bathurst, Orange, Dubbo, Mudgee, Parkes, Broken Hill
        // Starts at 2787 — BLUE_MOUNTAINS ends at 2786
        add(2787, 2899, SydneyRegion.CENTRAL_WEST);
    }

    private static void add(int start, int end, SydneyRegion region) {
        for (Map.Entry<Integer, RangeDefinition> entry : RANGE_MAP.entrySet()) {
            int existingEnd = entry.getKey();
            int existingStart = entry.getValue().start;
            if (start <= existingEnd && end >= existingStart) {
                throw new IllegalStateException(String.format(
                    "Overlap detected: new range [%d-%d] clashes with existing [%d-%d] (%s)",
                    start, end, existingStart, existingEnd, entry.getValue().region));
            }
        }
        RANGE_MAP.put(end, new RangeDefinition(start, region));
    }

    public static SydneyRegion classify(String suburbName, String postcode) {
        if (suburbName != null && !suburbName.isBlank() && postcode != null) {
            Map<String, SydneyRegion> byPostcode = NAME_OVERRIDES.get(suburbName.trim().toLowerCase());
            if (byPostcode != null) {
                SydneyRegion override = byPostcode.get(postcode.trim());
                if (override != null) return override;
            }
        }
        return classify(postcode);
    }

    public static SydneyRegion classify(String postcode) {
        if (postcode == null || postcode.isBlank()) return SydneyRegion.OTHER_NSW;
        try {
            int code = Integer.parseInt(postcode.trim());
            Map.Entry<Integer, RangeDefinition> entry = RANGE_MAP.ceilingEntry(code);
            if (entry != null && code >= entry.getValue().start) {
                return entry.getValue().region;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid postcode '{}' — defaulting to OTHER_NSW", postcode);
        }
        return SydneyRegion.OTHER_NSW;
    }
}
