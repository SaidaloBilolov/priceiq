package com.priceiq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class YandexMarketService {

    private static final Logger log = LoggerFactory.getLogger(YandexMarketService.class);

    @Value("${yandex.market.api-key}")
    private String apiKey;

    @Value("${yandex.market.api-url:https://api.partner.market.yandex.ru}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public YandexMarketService() {
        this.restTemplate = new RestTemplate();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Key", apiKey);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        headers.set("User-Agent", "PriceIQ-Backend/1.0");
        return headers;
    }

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "Yandex.Market API Integration");
        result.put("configuredApiUrl", apiUrl);
        result.put("apiKeyConfigured", (apiKey != null && !apiKey.isEmpty()));
        result.put("apiKeyPrefix", (apiKey != null && apiKey.length() > 8) ? apiKey.substring(0, 8) + "..." : "N/A");

        try {
            String url = apiUrl + "/v2/campaigns";
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            result.put("statusCode", response.getStatusCode().value());
            result.put("success", response.getStatusCode().is2xxSuccessful());
            result.put("rawResponseBody", response.getBody());
        } catch (Exception e) {
            log.error("Yandex.Market API connection test error: ", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    public ResponseEntity<String> getCampaigns() {
        String url = apiUrl + "/v2/campaigns";
        log.info("Requesting Yandex.Market campaigns from URL: {}", url);
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    public ResponseEntity<String> searchProducts(String query, Integer page, Integer pageSize) {
        String url = UriComponentsBuilder.fromHttpUrl(apiUrl + "/v2/models")
                .queryParam("query", query != null ? query.trim() : "")
                .queryParam("page", page != null ? page : 1)
                .queryParam("pageSize", pageSize != null ? pageSize : 10)
                .build()
                .toUriString();

        log.info("Searching Yandex.Market products for query: '{}' at URL: {}", query, url);
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }
}
