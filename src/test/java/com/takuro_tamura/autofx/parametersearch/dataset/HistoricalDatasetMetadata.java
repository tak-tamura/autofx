package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.parametersearch.config.MarketPriceType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 後続のパラメータ探索で、使用データの出所・期間・内容を特定するための再現性情報。
 * requestedFrom/ToはGMOコインのAPI日付、first/lastCandleTimeは実際のJST足時刻を表す。
 */
public record HistoricalDatasetMetadata(
    String datasetId,
    String sha256,
    String source,
    CurrencyPair currencyPair,
    TimeFrame timeFrame,
    MarketPriceType priceType,
    String timeZone,
    LocalDate requestedFrom,
    LocalDate requestedTo,
    LocalDateTime firstCandleTime,
    LocalDateTime lastCandleTime,
    int candleCount,
    long dataGapCount,
    int duplicateCount,
    Instant fetchedAt
) {
}
