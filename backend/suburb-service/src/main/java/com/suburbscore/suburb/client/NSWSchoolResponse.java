package com.suburbscore.suburb.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NSWSchoolResponse(
        @JsonProperty("School_name") String schoolName,
        @JsonProperty("Level_of_schooling") String schoolType,
        @JsonProperty("Town_suburb") String suburb,
        @JsonProperty("ICSEA_value") String icseaValue,
        @JsonProperty("Latitude") String latitude,
        @JsonProperty("Longitude") String longitude
) {}
