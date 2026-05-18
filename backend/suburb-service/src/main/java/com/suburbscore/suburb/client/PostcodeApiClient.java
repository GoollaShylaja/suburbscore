package com.suburbscore.suburb.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostcodeApiClient {

    private final RestTemplate restTemplate;

    @Value("${postcodeapi.base-url:http://v0.postcodeapi.com.au}")
    private String baseUrl;

    /**
     * Fetches all suburb entries for a specific postcode from the Postcode API.
     * Use this to enrich or add suburbs for a postcode not covered by the CSV seed.
     */
    public List<PostcodeApiResponse> fetchByPostcode(int postcode) {
        String url = baseUrl + "/suburbs/" + postcode + ".json";
        log.info("Fetching suburbs for postcode {} from {}", postcode, url);
        try {
            List<PostcodeApiResponse> result = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<PostcodeApiResponse>>() {}
            ).getBody();
            if (result == null || result.isEmpty()) {
                log.warn("No suburbs found for postcode {}", postcode);
                return Collections.emptyList();
            }
            log.info("Postcode API returned {} suburb(s) for postcode {}", result.size(), postcode);
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch postcode {}: {}", postcode, e.getMessage());
            return Collections.emptyList();
        }
    }
}
