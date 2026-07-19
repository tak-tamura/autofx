package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 取得データをバックテストへ渡す前に、再現性や時系列の正しさを壊す異常を検出する。
 * 不正データを並べ替えたり補正したりせず、原因を確認できるよう例外で停止する。
 */
public class HistoricalDatasetValidator {

    public DatasetValidationReport validate(
        List<Candle> candles,
        ParameterSearchSpecification specification
    ) {
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Historical dataset must not be empty");
        }

        // Setで重複を検出しつつ、入力順そのものが昇順かはpreviousとの比較で検証する。
        final Set<LocalDateTime> timestamps = new HashSet<>();
        long missingIntervals = 0;
        Candle previous = null;
        for (Candle candle : candles) {
            validateIdentity(candle, specification.marketData());
            validateTime(candle, specification);
            validateOhlc(candle);
            if (!timestamps.add(candle.getTime())) {
                throw new IllegalArgumentException("Duplicate candle timestamp: " + candle.getTime());
            }
            if (previous != null) {
                if (!candle.getTime().isAfter(previous.getTime())) {
                    throw new IllegalArgumentException("Candles must be strictly ordered by time");
                }
                final LocalDateTime expected = HistoricalDatasetFetcher.nextOpenTime(
                    previous.getTime(),
                    specification.marketData().timeFrame()
                );
                if (candle.getTime().isAfter(expected)) {
                    // 週末・休場とデータ欠損はまだ区別せず、後から調査できるよう空白区間として記録する。
                    missingIntervals++;
                }
            }
            previous = candle;
        }

        return new DatasetValidationReport(
            candles.size(),
            candles.get(0).getTime(),
            candles.get(candles.size() - 1).getTime(),
            missingIntervals
        );
    }

    private void validateTime(Candle candle, ParameterSearchSpecification specification) {
        final var conditions = specification.marketData();
        if (!conditions.timeFrame().truncateTime(candle.getTime()).equals(candle.getTime())) {
            throw new IllegalArgumentException("Candle timestamp is not aligned to its timeframe: " + candle.getTime());
        }
        // 暦日ではなく、JST 6:00に切り替わるGMOコインのAPI日付で取得範囲を検証する。
        final var apiDate = GmoCoinApiDate.fromCandleTime(candle.getTime());
        if (apiDate.isBefore(specification.periods().datasetFrom())
            || apiDate.isAfter(specification.periods().datasetTo())) {
            throw new IllegalArgumentException("Candle timestamp is outside requested dataset period: " + candle.getTime());
        }
    }

    private void validateIdentity(Candle candle, ParameterSearchSpecification.MarketDataConditions conditions) {
        if (candle == null) {
            throw new IllegalArgumentException("Historical dataset contains a null candle");
        }
        if (candle.getCurrencyPair() != conditions.currencyPair()
            || candle.getTimeFrame() != conditions.timeFrame()) {
            throw new IllegalArgumentException("Candle market identity does not match dataset conditions");
        }
    }

    private void validateOhlc(Candle candle) {
        if (candle.getOpen() == null || candle.getHigh() == null
            || candle.getLow() == null || candle.getClose() == null) {
            throw new IllegalArgumentException("Candle OHLC values are required at " + candle.getTime());
        }
        if (candle.getOpen().getValue().signum() <= 0
            || candle.getHigh().getValue().signum() <= 0
            || candle.getLow().getValue().signum() <= 0
            || candle.getClose().getValue().signum() <= 0) {
            throw new IllegalArgumentException("Candle OHLC values must be positive at " + candle.getTime());
        }
        if (candle.getLow().compareTo(candle.getOpen()) > 0
            || candle.getLow().compareTo(candle.getClose()) > 0
            || candle.getHigh().compareTo(candle.getOpen()) < 0
            || candle.getHigh().compareTo(candle.getClose()) < 0) {
            throw new IllegalArgumentException("Invalid OHLC relationship at " + candle.getTime());
        }
    }
}
