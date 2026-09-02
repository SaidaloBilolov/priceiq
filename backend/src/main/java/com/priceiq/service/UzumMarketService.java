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
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UzumMarketService {

    private static final Logger log = LoggerFactory.getLogger(UzumMarketService.class);
    private final RestTemplate restTemplate;
    private static final String UZUM_REST_URL = "https://api.uzum.uz/api/v2/product/search";
    private static final String UZUM_GRAPHQL_URL = "https://graphql.uzum.uz/";

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

        // --- Approach 1: Uzum Mobile Direct Endpoint ---
        try {
            String url = UriComponentsBuilder.fromHttpUrl(UZUM_REST_URL)
                    .queryParam("query", query.trim())
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

            System.out.println("UZUM STATUS CODE: " + rawResponse.getStatusCode());
            System.out.println("UZUM RAW BODY: " + rawResponse.getBody());

            log.info("UZUM STATUS CODE: {}", rawResponse.getStatusCode());
            log.info("UZUM RAW BODY: {}", rawResponse.getBody());

            if (rawResponse.getStatusCode().is2xxSuccessful() && rawResponse.getBody() != null && !rawResponse.getBody().contains("<html")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                UzumSearchResponseDto responseDto = mapper.readValue(rawResponse.getBody(), UzumSearchResponseDto.class);

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
            }
        } catch (Exception e) {
            System.err.println("UZUM API REST ERROR: " + e.getMessage());
            log.error("UZUM API REST ERROR: ", e);
        }

        // --- Approach 2: Uzum Web Search Endpoint ---
        try {
            String webUrl = "https://uzum.uz/api/v2/product/search?query=" + java.net.URLEncoder.encode(query.trim(), "UTF-8") + "&size=10";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Referer", "https://uzum.uz/");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> webResponse = restTemplate.exchange(webUrl, HttpMethod.GET, requestEntity, String.class);

            System.out.println("UZUM WEB STATUS CODE: " + webResponse.getStatusCode());
            System.out.println("UZUM WEB RAW BODY: " + webResponse.getBody());

            if (webResponse.getStatusCode().is2xxSuccessful() && webResponse.getBody() != null && !webResponse.getBody().contains("<html")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                UzumSearchResponseDto responseDto = mapper.readValue(webResponse.getBody(), UzumSearchResponseDto.class);

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
            }
        } catch (Exception e) {
            System.err.println("UZUM WEB API ERROR: " + e.getMessage());
            log.error("UZUM WEB API ERROR: ", e);
        }

        // --- Approach 3: Public CORS Proxy / Scraper Gateway ---
        try {
            String targetUzumUrl = "https://api.uzum.uz/api/v2/product/search?query=" + java.net.URLEncoder.encode(query.trim(), "UTF-8") + "&size=10";
            String proxyUrl = "https://api.allorigins.win/raw?url=" + java.net.URLEncoder.encode(targetUzumUrl, "UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> proxyResponse = restTemplate.exchange(proxyUrl, HttpMethod.GET, requestEntity, String.class);

            System.out.println("UZUM PROXY STATUS CODE: " + proxyResponse.getStatusCode());
            System.out.println("UZUM PROXY RAW BODY: " + proxyResponse.getBody());

            log.info("UZUM PROXY STATUS CODE: {}", proxyResponse.getStatusCode());
            log.info("UZUM PROXY RAW BODY: {}", proxyResponse.getBody());

            if (proxyResponse.getStatusCode().is2xxSuccessful() && proxyResponse.getBody() != null && !proxyResponse.getBody().contains("<html")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                UzumSearchResponseDto responseDto = mapper.readValue(proxyResponse.getBody(), UzumSearchResponseDto.class);

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
            }
        } catch (Exception e) {
            System.err.println("UZUM API PROXY ERROR: " + e.getMessage());
            log.error("UZUM API PROXY ERROR: ", e);
        }

        return new ArrayList<>();
    }
}
