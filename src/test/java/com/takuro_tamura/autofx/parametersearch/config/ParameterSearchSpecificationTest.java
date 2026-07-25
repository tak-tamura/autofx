package com.takuro_tamura.autofx.parametersearch.config;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ParameterSearchSpecificationTest {

    @Test
    void loadsReviewablePhaseOneConditions() {
        final ParameterSearchSpecification specification =
            ParameterSearchSpecificationLoader.load("parameter-search.properties");

        assertThat(specification.marketData().currencyPair()).isEqualTo(CurrencyPair.USD_JPY);
        assertThat(specification.marketData().timeFrame()).isEqualTo(TimeFrame.HOUR);
        assertThat(specification.marketData().priceType()).isEqualTo(MarketPriceType.ASK);
        assertThat(specification.marketData().timeZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
        assertThat(specification.marketData().excludeIncompleteCandle()).isTrue();

        // 日付そのものは運用時に変更されるため、期間分割の不変条件を検証する。
        assertThat(specification.periods().datasetFrom()).isEqualTo(specification.periods().inSampleFrom());
        assertThat(specification.periods().datasetTo()).isEqualTo(specification.periods().outOfSampleTo());
        assertThat(specification.periods().inSampleTo().plusDays(1))
            .isEqualTo(specification.periods().outOfSampleFrom());

        assertThat(specification.strategySearchSpace().mode()).isEqualTo(SearchMode.ONE_FACTOR_AT_A_TIME);
        assertThat(specification.strategySearchSpace().maxCandidates())
            .isGreaterThanOrEqualTo(specification.strategySearchSpace().candidateCount());
        assertThat(specification.riskParameters().atrPeriod()).isPositive();
        assertThat(specification.riskParameters().stopMultiplier()).isPositive();
        assertThat(specification.riskParameters().profitMultiplier()).isPositive();
        assertThat(specification.selectionCriteria().minimumTrades()).isPositive();
        assertThat(specification.selectionCriteria().maximumSelectedCandidates()).isPositive();
        assertThat(specification.walkForwardCriteria().windowMonths()).isPositive();
        assertThat(specification.walkForwardCriteria().minimumTradesPerWindow()).isPositive();
        assertThat(specification.walkForwardCriteria().minimumProfitableWindowRate())
            .isBetween(BigDecimal.ZERO, BigDecimal.ONE);
    }

    @Test
    void keepsSearchExecutionAssumptionsAlignedWithBacktestEngine() {
        final ParameterSearchSpecification specification =
            ParameterSearchSpecificationLoader.load("parameter-search.properties");

        assertThat(specification.executionAssumptions()).isEqualTo(BacktestAssumptions.current());
    }

    @Test
    void rejectsOverlappingEvaluationPeriods() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            new ParameterSearchSpecification.EvaluationPeriods(
                java.time.LocalDate.of(2023, 1, 1),
                java.time.LocalDate.of(2025, 12, 31),
                java.time.LocalDate.of(2023, 1, 1),
                java.time.LocalDate.of(2024, 12, 31),
                java.time.LocalDate.of(2024, 12, 31),
                java.time.LocalDate.of(2025, 12, 31)
            )
        );
    }

    @Test
    void requiresIncompleteCandlesToBeExcluded() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            new ParameterSearchSpecification.MarketDataConditions(
                CurrencyPair.USD_JPY,
                TimeFrame.HOUR,
                MarketPriceType.ASK,
                ZoneId.of("Asia/Tokyo"),
                false
            )
        );
    }
}
