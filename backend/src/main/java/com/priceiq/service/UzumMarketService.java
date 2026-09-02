package com.priceiq.service;

import com.priceiq.dto.UzumProductDto;
import com.priceiq.dto.UzumSearchResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class UzumMarketService {

    private static final Logger log = LoggerFactory.getLogger(UzumMarketService.class);
    private final RestTemplate restTemplate;
    private static final String UZUM_SEARCH_URL = "https://api.uzum.uz/api/v2/product/search";

    public UzumMarketService() {
        this.restTemplate = new RestTemplate();
    }

    public List<UzumProductDto> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(UZUM_SEARCH_URL)
                    .queryParam("query", query.trim())
                    .queryParam("size", 10)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
            headers.set("Accept", "application/json");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<UzumSearchResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    UzumSearchResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getPayload() != null) {
                List<UzumSearchResponseDto.ProductItem> items = response.getBody().getPayload().getItemList();
                if (items != null) {
                    List<UzumProductDto> dtos = new ArrayList<>();
                    for (UzumSearchResponseDto.ProductItem item : items) {
                        dtos.add(item.toUzumProductDto());
                    }
                    return dtos;
                }
            } else {
                log.error("Uzum Market API returned non-200 status code [{}] for query: [{}]", response.getStatusCode(), query);
            }
        } catch (Exception e) {
            log.error("Exception during Uzum Market API search for query [{}]: {}", query, e.getMessage());
        }

        return new ArrayList<>();
    }
}
