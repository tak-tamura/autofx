package com.takuro_tamura.autofx.infrastructure.external.adapter;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.infrastructure.external.exception.ApiErrorException;
import com.takuro_tamura.autofx.infrastructure.external.response.ApiResponse;
import com.takuro_tamura.autofx.infrastructure.external.response.Kline;
import com.takuro_tamura.autofx.infrastructure.external.response.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

@Component
public class PublicApi {

    public PublicApi(
        @Value("${public-api.ticker}") String tickerUrl,
        @Value("${public-api.klines}") String klinesUrl,
        RestClient restClient
    ) {
        this.tickerUrl = tickerUrl;
        this.klinesUrl = klinesUrl;
        this.restClient = restClient;
    }

    private final Logger log = LoggerFactory.getLogger(PublicApi.class);
    private final String tickerUrl;
    private final String klinesUrl;

    private final RestClient restClient;

    public List<Ticker> getTickers() {
        try {
            final ApiResponse<List<Ticker>> response = restClient.get()
                .uri(tickerUrl)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

            if (response == null) {
                throw new ApiErrorException("Ticker API call failed, response body is null");
            }

            if (response.getStatus() != 0) {
                throw new ApiErrorException("Ticker API call failed, status:" + response.getStatus());
            }

            return response.getData();
        } catch (RestClientResponseException e) {
            throw new ApiErrorException("Ticker API call failed", e);
        }
    }

    public List<Kline> getKlines(CurrencyPair currencyPair, TimeFrame timeFrame, String date) {
        try {
            final URI uri = UriComponentsBuilder.fromUriString(klinesUrl)
                .queryParam("priceType", "ASK")
                .queryParam("interval", timeFrame.getFullLabel())
                .queryParam("date", date)
                .queryParam("symbol", currencyPair.name())
                .build()
                .toUri();

            final ApiResponse<List<Kline>> response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

            if (response == null) {
                throw new ApiErrorException("Ticker API call failed, response body is null");
            }

            if (response.getStatus() != 0) {
                throw new ApiErrorException("Ticker API call failed, status:" + response.getStatus());
            }

            return response.getData();
        } catch (RestClientResponseException e) {
            throw new ApiErrorException("Ticker API call failed", e);
        }
    }
}
