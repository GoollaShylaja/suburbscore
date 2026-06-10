package com.suburbscore.suburb.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class PostcodeApiClient {

    private final WebClient webClient;

    @Value("${postcodeapi.base-url:http://v0.postcodeapi.com.au}")
    private String baseUrl;

    public PostcodeApiClient(@Qualifier("externalWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetches all suburb entries for a specific postcode from the Postcode API.
     * Use this to enrich or add suburbs for a postcode not covered by the CSV seed.
     */
    public List<PostcodeApiResponse> fetchByPostcode(int postcode) {
        String url = baseUrl + "/suburbs/" + postcode + ".json";
        log.info("Fetching suburbs for postcode {} from {}", postcode, url);
        try {
            List<PostcodeApiResponse> result = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<PostcodeApiResponse>>() {})
                    .block();

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
