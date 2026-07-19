package com.takuro_tamura.autofx.parametersearch.dataset;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KlineCandleMapperTest {

    @Test
    void mapsKlineUsingConfiguredMarketIdentityAndTimeZone() {
        final var specification = DatasetTestFixtures.specification();
        final var kline = DatasetTestFixtures.kline(
            LocalDateTime.of(2025, 1, 1, 9, 0),
            "150.100", "150.300", "150.000", "150.200"
        );

        final var candle = new KlineCandleMapper().map(kline, specification.marketData());

        assertThat(candle.getTime()).isEqualTo(LocalDateTime.of(2025, 1, 1, 9, 0));
        assertThat(candle.getCurrencyPair()).isEqualTo(specification.marketData().currencyPair());
        assertThat(candle.getTimeFrame()).isEqualTo(specification.marketData().timeFrame());
        assertThat(candle.getOpen().getValue()).isEqualByComparingTo("150.100");
        assertThat(candle.getHigh().getValue()).isEqualByComparingTo("150.300");
        assertThat(candle.getLow().getValue()).isEqualByComparingTo("150.000");
        assertThat(candle.getClose().getValue()).isEqualByComparingTo("150.200");
    }
}
