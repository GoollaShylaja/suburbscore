package com.suburbscore.suburb.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PostcodeApiResponse(
        String name,
        int postcode,
        @JsonAlias({"latitude", "lat"}) double latitude,
        @JsonAlias({"longitude", "lon", "long"}) double longitude,
        State state
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record State(String name, String abbreviation) {}
}
