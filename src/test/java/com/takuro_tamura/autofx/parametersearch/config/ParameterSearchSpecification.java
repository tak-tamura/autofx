package com.takuro_tamura.autofx.parametersearch.config;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * パラメータ探索の入力条件をまとめたテスト専用の仕様。
 * ライブ設定やDB設定を変更せず、探索実行ごとの条件を固定する。
 */
public record ParameterSearchSpecification(
    MarketDataConditions marketData,
    EvaluationPeriods periods,
    BacktestAssumptions executionAssumptions,
    StrategySearchSpace strategySearchSpace
) {
    public ParameterSearchSpecification {
        if (marketData == null || periods == null || executionAssumptions == null || strategySearchSpace == null) {
            throw new IllegalArgumentException("All parameter-search conditions are required");
        }
    }

    public record MarketDataConditions(
        CurrencyPair currencyPair,
        TimeFrame timeFrame,
        MarketPriceType priceType,
        ZoneId timeZone,
        boolean excludeIncompleteCandle
    ) {
        public MarketDataConditions {
            if (currencyPair == null || timeFrame == null || priceType == null || timeZone == null) {
                throw new IllegalArgumentException("All market-data conditions are required");
            }
            if (!excludeIncompleteCandle) {
                throw new IllegalArgumentException("Incomplete candles must be excluded from parameter searches");
            }
        }
    }

    public record EvaluationPeriods(
        LocalDate datasetFrom,
        LocalDate datasetTo,
        LocalDate inSampleFrom,
        LocalDate inSampleTo,
        LocalDate outOfSampleFrom,
        LocalDate outOfSampleTo
    ) {
        public EvaluationPeriods {
            if (datasetFrom == null || datasetTo == null
                || inSampleFrom == null || inSampleTo == null
                || outOfSampleFrom == null || outOfSampleTo == null) {
                throw new IllegalArgumentException("All evaluation-period dates are required");
            }
            if (datasetFrom.isAfter(datasetTo)
                || inSampleFrom.isAfter(inSampleTo)
                || outOfSampleFrom.isAfter(outOfSampleTo)) {
                throw new IllegalArgumentException("Evaluation-period start must not be after its end");
            }
            if (!datasetFrom.equals(inSampleFrom) || !datasetTo.equals(outOfSampleTo)) {
                throw new IllegalArgumentException("Dataset bounds must match the complete evaluation period");
            }
            if (!inSampleTo.plusDays(1).equals(outOfSampleFrom)) {
                throw new IllegalArgumentException("In-sample and out-of-sample periods must be contiguous and non-overlapping");
            }
        }
    }
}
