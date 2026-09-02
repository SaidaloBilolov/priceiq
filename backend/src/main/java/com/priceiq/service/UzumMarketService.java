package com.priceiq.service;

import com.priceiq.dto.UzumProductDto;
import com.priceiq.dto.UzumSearchResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Service
public class UzumMarketService {

    private static final Logger log = LoggerFactory.getLogger(UzumMarketService.class);
    private final RestTemplate restTemplate;

    private static final String UZUM_REST_URL = "https://api.uzum.uz/api/v2/product/search";
    private static final String UZUM_WEB_URL = "https://uzum.uz/api/v2/product/search";

    public UzumMarketService() {
        this.restTemplate = createTrustAllRestTemplate();
    }

    private RestTemplate createTrustAllRestTemplate() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                        ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                    }
                    connection.setInstanceFollowRedirects(true);
                    super.prepareConnection(connection, httpMethod);
                }
            };
            factory.setConnectTimeout(6000);
            factory.setReadTimeout(12000);
            return new RestTemplate(factory);
        } catch (Exception e) {
            log.error("Failed to configure trust-all RestTemplate: {}", e.getMessage(), e);
            return new RestTemplate();
        }
    }

    public List<UzumProductDto> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String trimmedQuery = query.trim();

        // 1. Primary Attempt: AllOrigins Public Scraper Proxy Gateway (Bypasses Datacenter IP blocking)
        try {
            String targetUzumUrl = UZUM_REST_URL + "?query=" + URLEncoder.encode(trimmedQuery, StandardCharsets.UTF_8) + "&size=10";
            String proxyUrl = "https://api.allorigins.win/raw?url=" + URLEncoder.encode(targetUzumUrl, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> proxyResponse = restTemplate.exchange(proxyUrl, HttpMethod.GET, requestEntity, String.class);

            if (proxyResponse.getStatusCode().is2xxSuccessful() && proxyResponse.getBody() != null && !proxyResponse.getBody().contains("<html")) {
                List<UzumProductDto> parsed = parseJsonResponse(proxyResponse.getBody());
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.error("Uzum Scraper Proxy Gateway Attempt Error: ", e.getMessage());
        }

        // 2. Secondary Attempt: Direct Uzum Mobile API
        try {
            String url = UriComponentsBuilder.fromHttpUrl(UZUM_REST_URL)
                    .queryParam("query", trimmedQuery)
                    .queryParam("size", 10)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Uzum/2.28.0 (Android 13; Mobile)");
            headers.set("X-Forwarded-For", "213.230.96.15");
            headers.set("X-Real-IP", "213.230.96.15");
            headers.set("Accept", "application/json");
            headers.set("Accept-Language", "uz-UZ,uz;q=0.9,ru-RU;q=0.8");
            headers.set("Origin", "https://uzum.uz");
            headers.set("Referer", "https://uzum.uz/");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> rawResponse = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

            if (rawResponse.getStatusCode().is2xxSuccessful() && rawResponse.getBody() != null && !rawResponse.getBody().contains("<html")) {
                List<UzumProductDto> parsed = parseJsonResponse(rawResponse.getBody());
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.error("Uzum Direct Mobile API Attempt Error: ", e.getMessage());
        }

        // 3. Tertiary Attempt: Uzum Direct Web Gateway
        try {
            String webUrl = UZUM_WEB_URL + "?query=" + URLEncoder.encode(trimmedQuery, StandardCharsets.UTF_8) + "&size=10";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Referer", "https://uzum.uz/");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> webResponse = restTemplate.exchange(webUrl, HttpMethod.GET, requestEntity, String.class);

            if (webResponse.getStatusCode().is2xxSuccessful() && webResponse.getBody() != null && !webResponse.getBody().contains("<html")) {
                List<UzumProductDto> parsed = parseJsonResponse(webResponse.getBody());
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.error("Uzum Direct Web API Attempt Error: ", e.getMessage());
        }

        // Strictly return empty list [] if zero items returned
        return new ArrayList<>();
    }

    private List<UzumProductDto> parseJsonResponse(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            UzumSearchResponseDto responseDto = mapper.readValue(json, UzumSearchResponseDto.class);

            if (responseDto != null && responseDto.getPayload() != null) {
                List<UzumSearchResponseDto.ProductItem> items = responseDto.getPayload().getItemList();
                if (items != null && !items.isEmpty()) {
                    List<UzumProductDto> dtos = new ArrayList<>();
                    for (UzumSearchResponseDto.ProductItem item : items) {
                        dtos.add(item.toUzumProductDto());
                    }
                    return dtos;
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Uzum JSON response: ", e.getMessage());
        }
        return new ArrayList<>();
    }
}
