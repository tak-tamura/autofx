package com.takuro_tamura.autofx.parametersearch.config;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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

        assertThat(specification.periods().datasetFrom()).isEqualTo(LocalDate.of(2023, 10, 28));
        assertThat(specification.periods().datasetTo()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(specification.periods().inSampleTo()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(specification.periods().outOfSampleFrom()).isEqualTo(LocalDate.of(2025, 1, 1));

        assertThat(specification.strategySearchSpace().mode()).isEqualTo(SearchMode.ONE_FACTOR_AT_A_TIME);
        assertThat(specification.strategySearchSpace().maxCandidates()).isEqualTo(100);
        assertThat(specification.strategySearchSpace().baseline().toStrategyConfig().emaPeriod1()).isEqualTo(8);
        assertThat(specification.riskParameters().atrPeriod()).isEqualTo(14);
        assertThat(specification.riskParameters().stopMultiplier()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(specification.riskParameters().profitMultiplier()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(specification.selectionCriteria().minimumTrades()).isEqualTo(30);
        assertThat(specification.selectionCriteria().maximumSelectedCandidates()).isEqualTo(5);
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
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2024, 12, 31),
                LocalDate.of(2024, 12, 31),
                LocalDate.of(2025, 12, 31)
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
