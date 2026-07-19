package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.infrastructure.external.response.Kline;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * GMOコインAPI固有のKlineレスポンスを、既存バックテストが扱うCandleへ変換する。
 * 通貨ペア・時間足はレスポンスに含まれないため、Phase 1で固定した探索条件から補完する。
 */
public class KlineCandleMapper {

    public Candle map(Kline kline, ParameterSearchSpecification.MarketDataConditions conditions) {
        if (kline == null || conditions == null) {
            throw new IllegalArgumentException("Kline and market-data conditions are required");
        }
        if (kline.getOpen() == null || kline.getHigh() == null
            || kline.getLow() == null || kline.getClose() == null) {
            throw new IllegalArgumentException("Kline OHLC values are required");
        }

        // openTimeはepoch millisecondsなので、探索条件で明示したタイムゾーンへ境界で変換する。
        final LocalDateTime openTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(kline.getOpenTime()),
            conditions.timeZone()
        );
        return Candle.builder()
            .time(openTime)
            .currencyPair(conditions.currencyPair())
            .timeFrame(conditions.timeFrame())
            .open(new Price(kline.getOpen()))
            .high(new Price(kline.getHigh()))
            .low(new Price(kline.getLow()))
            .close(new Price(kline.getClose()))
            .build();
    }
}
