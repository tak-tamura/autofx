package com.takuro_tamura.autofx.parametersearch.selection;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.parametersearch.config.CandidateSelectionCriteria;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecificationLoader;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;
import com.takuro_tamura.autofx.parametersearch.execution.CandidateBacktestEvaluation;
import com.takuro_tamura.autofx.parametersearch.execution.InSampleParameterSearchResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InSampleCandidateRankerTest {

    @Test
    void selectsEligibleCandidatesWithoutUsingNetProfitAsTheOnlyRankingMetric() {
        final CandidateSelectionCriteria criteria = new CandidateSelectionCriteria(
            30, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, 2
        );
        final StrategyParameterSet baseline = ParameterSearchSpecificationLoader
            .load("parameter-search.properties").strategySearchSpace().baseline();
        final CandidateBacktestEvaluation highAverageR = evaluation(
            baseline, metrics(40, "100", "1.20", "0.30", "100")
        );
        final CandidateBacktestEvaluation highNetProfit = evaluation(
            withEmaShort(baseline, 7), metrics(50, "1000", "2.00", "0.20", "50")
        );
        final CandidateBacktestEvaluation tooFewTrades = evaluation(
            withEmaShort(baseline, 6), metrics(10, "5000", "5.00", "1.00", "10")
        );
        final InSampleParameterSearchResult searchResult = result(List.of(
            tooFewTrades, highNetProfit, highAverageR
        ));

        final InSampleCandidateSelection selection = new InSampleCandidateRanker().rank(searchResult, criteria);

        assertThat(selection.rankedCandidates()).extracting(RankedCandidate::evaluation)
            .containsExactly(highAverageR, highNetProfit, tooFewTrades);
        assertThat(selection.selectedCandidates()).extracting(RankedCandidate::evaluation)
            .containsExactly(highAverageR, highNetProfit);
        assertThat(selection.rankedCandidates().get(2).rejectionReasons())
            .containsExactly(CandidateRejectionReason.INSUFFICIENT_TRADES);
    }

    @Test
    void recordsEveryFailedEligibilityRule() {
        final CandidateSelectionCriteria criteria = new CandidateSelectionCriteria(
            30, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, 1
        );
        final StrategyParameterSet baseline = ParameterSearchSpecificationLoader
            .load("parameter-search.properties").strategySearchSpace().baseline();
        final CandidateBacktestEvaluation rejected = evaluation(
            baseline, metrics(5, "-100", "0.50", "-0.20", "200")
        );

        final RankedCandidate ranked = new InSampleCandidateRanker()
            .rank(result(List.of(rejected)), criteria)
            .rankedCandidates().get(0);

        assertThat(ranked.eligible()).isFalse();
        assertThat(ranked.selected()).isFalse();
        assertThat(ranked.rejectionReasons()).containsExactly(
            CandidateRejectionReason.INSUFFICIENT_TRADES,
            CandidateRejectionReason.NET_PROFIT_BELOW_MINIMUM,
            CandidateRejectionReason.PROFIT_FACTOR_BELOW_MINIMUM,
            CandidateRejectionReason.AVERAGE_R_BELOW_MINIMUM
        );
    }

    private InSampleParameterSearchResult result(List<CandidateBacktestEvaluation> evaluations) {
        return new InSampleParameterSearchResult(
            "fixed-dataset",
            LocalDateTime.of(2023, 10, 28, 0, 0),
            LocalDateTime.of(2025, 1, 1, 0, 0),
            evaluations
        );
    }

    private CandidateBacktestEvaluation evaluation(StrategyParameterSet parameters, BacktestMetrics metrics) {
        return new CandidateBacktestEvaluation(
            parameters,
            new BacktestResult(List.of(), BacktestAssumptions.current()),
            metrics
        );
    }

    private BacktestMetrics metrics(
        int trades,
        String netProfit,
        String profitFactor,
        String averageR,
        String maximumDrawdown
    ) {
        final BigDecimal net = new BigDecimal(netProfit);
        final BigDecimal average = new BigDecimal(averageR);
        return new BacktestMetrics(
            trades, trades / 2, trades / 2, 0, new BigDecimal("0.5"),
            net.max(BigDecimal.ZERO).add(new BigDecimal("100")),
            net.max(BigDecimal.ZERO).add(new BigDecimal("100")).subtract(net),
            net,
            BigDecimal.TEN,
            BigDecimal.TEN,
            Optional.of(new BigDecimal(profitFactor)),
            new BigDecimal(maximumDrawdown),
            3,
            3,
            List.of(average),
            Optional.of(average),
            new BigDecimal("0.1"),
            BigDecimal.ZERO
        );
    }

    private StrategyParameterSet withEmaShort(StrategyParameterSet source, int emaShort) {
        return new StrategyParameterSet(
            emaShort, source.emaLongPeriod(), source.rsiPeriod(), source.macdFastPeriod(),
            source.macdSlowPeriod(), source.macdSignalPeriod(), source.bBandsPeriod(),
            source.bBandsMultiplier(), source.adxPeriod(), source.adxThreshold()
        );
    }
}
