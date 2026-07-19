package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.infrastructure.external.adapter.PublicApi;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoricalDatasetFetcherTest {

    @Test
    void fetchesEachConfiguredDateAndExcludesIncompleteCandle() {
        final var specification = DatasetTestFixtures.specification();
        final PublicApi publicApi = mock(PublicApi.class);
        when(publicApi.getKlines(
            specification.marketData().currencyPair(), specification.marketData().timeFrame(), "20250101"
        )).thenReturn(List.of(DatasetTestFixtures.kline(
            LocalDateTime.of(2025, 1, 1, 23, 0), "150", "151", "149", "150.5"
        )));
        when(publicApi.getKlines(
            specification.marketData().currencyPair(), specification.marketData().timeFrame(), "20250102"
        )).thenReturn(List.of(DatasetTestFixtures.kline(
            LocalDateTime.of(2025, 1, 2, 0, 0), "150.5", "151", "150", "150.8"
        )));
        final Clock clock = Clock.fixed(Instant.parse("2025-01-01T15:30:00Z"), ZoneOffset.UTC);
        final HistoricalDatasetFetcher fetcher = new HistoricalDatasetFetcher(
            publicApi,
            new KlineCandleMapper(),
            clock,
            HistoricalDatasetFetcher.RequestPacer.noDelay()
        );

        final var candles = fetcher.fetch(specification);

        assertThat(candles).hasSize(1);
        assertThat(candles.get(0).getTime()).isEqualTo(LocalDateTime.of(2025, 1, 1, 23, 0));
        verify(publicApi).getKlines(
            specification.marketData().currencyPair(), specification.marketData().timeFrame(), "20250101"
        );
        verify(publicApi).getKlines(
            specification.marketData().currencyPair(), specification.marketData().timeFrame(), "20250102"
        );
    }
}
