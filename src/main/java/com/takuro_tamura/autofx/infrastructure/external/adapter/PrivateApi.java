package com.takuro_tamura.autofx.infrastructure.external.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.takuro_tamura.autofx.infrastructure.external.exception.ApiErrorException;
import com.takuro_tamura.autofx.infrastructure.external.exception.ApiLimitExceedException;
import com.takuro_tamura.autofx.infrastructure.external.request.*;
import com.takuro_tamura.autofx.infrastructure.external.response.ApiResponse;
import com.takuro_tamura.autofx.infrastructure.external.response.Assets;
import com.takuro_tamura.autofx.infrastructure.external.response.CancelBulkOrderResponse;
import com.takuro_tamura.autofx.infrastructure.external.response.OrderResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PrivateApi {
    private final Logger log = LoggerFactory.getLogger(PrivateApi.class);
    private final PrivateApiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Retryable(retryFor = {ApiLimitExceedException.class})
    public Assets getAssets() {
        final Assets asset = get(properties.path.assets, new ParameterizedTypeReference<>() {}, null);
        if (asset == null) {
            throw new ApiErrorException("Asset API returned empty response");
        }
        return asset;
    }

    @Retryable(retryFor = {ApiLimitExceedException.class})
    public OrderResponse order(OrderRequest request) {
        final List<OrderResponse> response = post(properties.path.order, new ParameterizedTypeReference<>() {}, request);
        if (response.isEmpty()) {
            throw new ApiErrorException("Order response is empty");
        }
        return response.get(0);
    }

    @Retryable(retryFor = {ApiLimitExceedException.class})
    public List<OrderResponse> closeOrder(CloseOrderRequest request) {
        final List<OrderResponse> response =  post(properties.path.closeOrder, new ParameterizedTypeReference<>() {}, request);
        if (response.isEmpty()) {
            throw new ApiErrorException("Close order response is empty");
        }
        return response;
    }

    @Retryable(retryFor = {ApiLimitExceedException.class})
    public CancelBulkOrderResponse cancelBulkOrder(CancelBulkOrderRequest request) {
        return post(properties.path.cancelBulkOrder, new ParameterizedTypeReference<>() {}, request);
    }

    @Retryable(retryFor = {ApiLimitExceedException.class})
    public String issueWsToken() {
        return post(properties.path.wsAuth, new ParameterizedTypeReference<>() {}, new Empty());
    }

    @Retryable(retryFor = {ApiLimitExceedException.class})
    public void extendWsToken(String token) {
        final ExtendWsTokenRequest request = new ExtendWsTokenRequest(token);
        put(properties.path.wsAuth, new ParameterizedTypeReference<>() {}, request);
    }

    private <T> T get(String path, ParameterizedTypeReference<ApiResponse<T>> typeReference, MultiValueMap<String, String> queryParams) {
        try {
            final MultiValueMap<String, String> headerMap = createAuthHeader(path, HttpMethod.GET, null);

            final ApiResponse<T> response;
            if (queryParams == null) {
                response = restClient.get()
                    .uri(properties.host + path)
                    .headers(headers -> headers.addAll(headerMap))
                    .retrieve()
                    .body(typeReference);
            } else {
                response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.host(properties.host).path(path).queryParams(queryParams).build())
                    .headers(headers -> headers.addAll(headerMap))
                    .retrieve()
                    .body(typeReference);
            }

            if (response == null) {
                throw new ApiErrorException("[GET] Private call failed, response body is null");
            }

            if (response.getStatus() != 0) {
                if (response.isTooManyRequests()) {
                    throw new ApiLimitExceedException();
                }
                log.error("[GET] Private API returned error response: {}", response);
                throw new ApiErrorException("[GET] Private API call failed, status:" + response.getStatus());
            }

            return response.getData();
        } catch (Exception e) {
            throw new ApiErrorException("[GET] Private API call failed", e);
        }
    }

    private <T, U> T post(String path, ParameterizedTypeReference<ApiResponse<T>> typeReference, U body) {
        try {
            final MultiValueMap<String, String> headerMap = createAuthHeader(path, HttpMethod.POST, body);

            final ApiResponse<T> response = restClient.post()
                .uri(properties.host + path)
                .headers(headers -> headers.addAll(headerMap))
                .body(body)
                .retrieve()
                .body(typeReference);

            if (response == null) {
                throw new ApiErrorException("[POST] Private call failed, response body is null");
            }

            if (response.getStatus() != 0) {
                if (response.isTooManyRequests()) {
                    throw new ApiLimitExceedException();
                }
                log.error("[POST] Private API returned error response: {}", response);
                throw new ApiErrorException("[POST] Private API call failed, status:" + response.getStatus());
            }

            return response.getData();
        } catch (Exception e) {
            throw new ApiErrorException("[POST] Private API call failed", e);
        }
    }

    private <T, U> T put(String path, ParameterizedTypeReference<ApiResponse<T>> typeReference, U body) {
        try {
            final MultiValueMap<String, String> headerMap = createAuthHeader(path, HttpMethod.PUT, null);

            final ApiResponse<T> response = restClient.put()
                .uri(properties.host + path)
                .headers(headers -> headers.addAll(headerMap))
                .body(body)
                .retrieve()
                .body(typeReference);

            if (response == null) {
                throw new ApiErrorException("[PUT] Private call failed, response body is null");
            }

            if (response.getStatus() != 0) {
                if (response.isTooManyRequests()) {
                    throw new ApiLimitExceedException();
                }
                log.error("[POST] Private API returned error response: {}", response);
                throw new ApiErrorException("[PUT] Private API call failed, status:" + response.getStatus());
            }

            return response.getData();
        } catch (Exception e) {
            throw new ApiErrorException("[PUT] Private API call failed", e);
        }
    }

    private <T> MultiValueMap<String, String> createAuthHeader(String path, HttpMethod method, T body) throws JsonProcessingException {
        final String requestString = body != null ? objectMapper.writeValueAsString(body) : "";
        final long timestamp = Instant.now().toEpochMilli();
        final String text = timestamp + method.toString() + path + requestString;
        final SecretKeySpec keySpec = new SecretKeySpec(properties.apiSecret.getBytes(), "HmacSHA256");

        final Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ApiErrorException("failed create sign because of invalid algorithm", e);
        }

        final String sign = new String(Hex.encode(mac.doFinal(text.getBytes())));

        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("API-KEY", properties.apiKey);
        headers.add("API-TIMESTAMP", String.valueOf(timestamp));
        headers.add("API-SIGN", sign);
        return headers;
    }

    @Data
    @ConfigurationProperties(prefix = "private-api")
    public static class PrivateApiProperties {
        private String apiKey;
        private String apiSecret;
        private String host;
        private Path path;

        @Data
        public static class Path {
            private String assets;
            private String order;
            private String closeOrder;
            private String cancelBulkOrder;
            private String executions;
            private String wsAuth;
        }
    }
}
