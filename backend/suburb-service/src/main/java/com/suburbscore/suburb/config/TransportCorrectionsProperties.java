package com.suburbscore.suburb.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "transport")
@Getter
@Setter
public class TransportCorrectionsProperties {

    private List<Correction> corrections = new ArrayList<>();

    @Getter
    @Setter
    public static class Correction {
        /** Suburb name — matched case-insensitively against the database. */
        private String suburb;
        /**
         * Postcode used to disambiguate when the same suburb name exists in multiple postcodes.
         * Required for any suburb that appears more than once in the dataset.
         * When provided, lookup uses (suburb_name + postcode); otherwise name-only.
         */
        private String postcode;
        /** Verified peak-hour door-to-door commute by train (mins). Null = do not override. */
        private Integer cbdCommuteMinsTrain;
        /** Override station name. Null = do not override. */
        private String nearestStation;
        /** Override walk-to-station minutes. Null = do not override. */
        private Integer trainStationWalkMins;
        /** Override ferry access flag. Null = do not override. */
        private Boolean hasFerryAccess;
    }
}
