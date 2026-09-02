package com.priceiq.service;

import com.priceiq.dto.UzumProductDto;
import com.priceiq.dto.UzumSearchResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
import java.util.List;

@Service
public class UzumMarketService {

    private static final Logger log = LoggerFactory.getLogger(UzumMarketService.class);
    private final RestTemplate restTemplate;
    private static final String UZUM_SEARCH_URL = "https://api.uzum.uz/api/v2/product/search";

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
                    super.prepareConnection(connection, httpMethod);
                }
            };
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(10000);
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

        try {
            String url = UriComponentsBuilder.fromHttpUrl(UZUM_SEARCH_URL)
                    .queryParam("query", query.trim())
                    .queryParam("size", 10)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Origin", "https://uzum.uz");
            headers.set("Referer", "https://uzum.uz/");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<UzumSearchResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    UzumSearchResponseDto.class
            );

            log.info("Uzum API Raw Response: {}", response.getBody());

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
                log.error("UZUM API ERROR: Non-200 status code [{}] for query: [{}]", response.getStatusCode(), query);
            }
        } catch (Exception e) {
            log.error("UZUM API ERROR: Exception during Uzum search for query [{}]: ", query, e);
        }

        return new ArrayList<>();
    }
}
