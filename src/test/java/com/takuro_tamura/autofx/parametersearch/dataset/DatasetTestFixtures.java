package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.infrastructure.external.response.Kline;
import com.takuro_tamura.autofx.parametersearch.config.MarketPriceType;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecificationLoader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

final class DatasetTestFixtures {
    private DatasetTestFixtures() {
    }

    static ParameterSearchSpecification specification() {
        return new ParameterSearchSpecification(
            new ParameterSearchSpecification.MarketDataConditions(
                CurrencyPair.USD_JPY,
                TimeFrame.HOUR,
                MarketPriceType.ASK,
                ZoneId.of("Asia/Tokyo"),
                true
            ),
            new ParameterSearchSpecification.EvaluationPeriods(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 2)
            ),
            BacktestAssumptions.current(),
            ParameterSearchSpecificationLoader.load("parameter-search.properties").riskParameters(),
            // データセット用fixtureでも、本番の探索設定と同じ検証済み候補空間を使用する。
            ParameterSearchSpecificationLoader.load("parameter-search.properties").strategySearchSpace(),
            ParameterSearchSpecificationLoader.load("parameter-search.properties").selectionCriteria(),
            ParameterSearchSpecificationLoader.load("parameter-search.properties").walkForwardCriteria()
        );
    }

    static Kline kline(LocalDateTime time, String open, String high, String low, String close) {
        final Kline kline = new Kline();
        kline.setOpenTime(ZonedDateTime.of(time, ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli());
        kline.setOpen(new BigDecimal(open));
        kline.setHigh(new BigDecimal(high));
        kline.setLow(new BigDecimal(low));
        kline.setClose(new BigDecimal(close));
        return kline;
    }

    static Candle candle(LocalDateTime time, String open, String high, String low, String close) {
        return Candle.builder()
            .time(time)
            .currencyPair(CurrencyPair.USD_JPY)
            .timeFrame(TimeFrame.HOUR)
            .open(new Price(open))
            .high(new Price(high))
            .low(new Price(low))
            .close(new Price(close))
            .build();
    }
}
