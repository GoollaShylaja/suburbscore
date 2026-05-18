package com.suburbscore.suburb.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class NSWSchoolsApiClient {

    private final RestTemplate restTemplate;

    public NSWSchoolsApiClient(@Qualifier("externalRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${nsw.schools.api.url:https://data.nsw.gov.au/data/api/3/action/datastore_search}")
    private String apiUrl;

    @Value("${nsw.schools.api.resource-id:3e6d5f6a-055c-440d-a690-fc0537c31095}")
    private String resourceId;

    @Value("${nsw.schools.api.page-size:1000}")
    private int pageSize;

    /**
     * Fetches every school record in a small number of paginated bulk requests,
     * instead of one request per suburb. ~2,800 records → 3 pages at limit=1000.
     */
    public List<NSWSchoolResponse> fetchAllSchools() {
        List<NSWSchoolResponse> all = new ArrayList<>();
        int offset = 0;

        while (true) {
            URI uri = URI.create(
                    apiUrl + "?resource_id=" + resourceId
                    + "&limit=" + pageSize
                    + "&offset=" + offset);

            try {
                DatastoreResponse response = restTemplate.getForObject(uri, DatastoreResponse.class);

                if (response == null || !Boolean.TRUE.equals(response.success())
                        || response.result() == null || response.result().records() == null) {
                    log.warn("NSW Schools API returned empty/failed response at offset {}", offset);
                    break;
                }

                List<NSWSchoolResponse> page = response.result().records();
                if (page.isEmpty()) break;

                all.addAll(page);
                log.debug("Fetched {} school records (offset={}, total so far={})", page.size(), offset, all.size());

                if (page.size() < pageSize) break; // last page

                offset += pageSize;
            } catch (Exception e) {
                log.error("Failed to fetch schools at offset {}: {}", offset, e.getMessage());
                break;
            }
        }

        log.info("NSW Schools bulk fetch complete — {} total records", all.size());
        return Collections.unmodifiableList(all);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DatastoreResponse(Boolean success, Result result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(List<NSWSchoolResponse> records) {}
}
