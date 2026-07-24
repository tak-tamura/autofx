package com.takuro_tamura.autofx.parametersearch.paper;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;

/**
 * paper適用前に人が取得して渡す、現在の取引設定の読み取り専用スナップショット。
 * DBを直接参照しないことで、Phase 10のタスクから設定が変更される経路を作らない。
 */
public record PaperCurrentConfiguration(
    CurrencyPair currencyPair,
    TimeFrame timeFrame,
    int atrPeriod,
    StrategyParameterSet strategyParameters
) {
    public PaperCurrentConfiguration {
        if (currencyPair == null || timeFrame == null || strategyParameters == null || atrPeriod <= 0) {
            throw new IllegalArgumentException("Complete current paper configuration is required");
        }
    }
}
