package com.takuro_tamura.autofx.parametersearch.walkforward;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetricsCalculator;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.service.BackTestService;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.strategy.EmaCrossStrategy;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;
import com.takuro_tamura.autofx.parametersearch.config.WalkForwardCriteria;
import com.takuro_tamura.autofx.parametersearch.outofsample.OutOfSampleCandidateEvaluation;
import com.takuro_tamura.autofx.parametersearch.outofsample.OutOfSampleEvaluationResult;
import com.takuro_tamura.autofx.parametersearch.selection.RankedCandidate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 7の固定候補をOOS期間内の連続ウィンドウで再生し、期間をまたいだ安定性を検証する。
 * 各ウィンドウで指標を再計算するが、パラメータの再探索・候補の入れ替え・ライブ有効化は行わない。
 */
public class WalkForwardEvaluationRunner {
    private static final int RATE_SCALE = 8;
    private final CandleService candleService;
    private final BackTestService backTestService;
    private final BacktestMetricsCalculator metricsCalculator;

    public WalkForwardEvaluationRunner(
        CandleService candleService,
        BackTestService backTestService,
        BacktestMetricsCalculator metricsCalculator
    ) {
        this.candleService = candleService;
        this.backTestService = backTestService;
        this.metricsCalculator = metricsCalculator;
    }

    public WalkForwardEvaluationResult run(
        List<Candle> dataset,
        OutOfSampleEvaluationResult outOfSampleResult,
        ParameterSearchSpecification specification
    ) {
        if (dataset == null || outOfSampleResult == null || specification == null) {
            throw new IllegalArgumentException("Dataset, Out-of-sample result, and specification are required");
        }
        if (dataset.stream().anyMatch(candle -> candle == null || candle.getTime() == null)) {
            throw new IllegalArgumentException("Dataset must not contain null candles or timestamps");
        }
        validateDataset(dataset, specification);
        if (outOfSampleResult.evaluations().isEmpty()) {
            throw new IllegalArgumentException("At least one fixed Out-of-sample candidate is required");
        }

        final LocalDateTime periodStart = specification.periods().outOfSampleFrom().atStartOfDay();
        final LocalDateTime periodEndExclusive = specification.periods().outOfSampleTo().plusDays(1).atStartOfDay();
        if (!periodStart.equals(outOfSampleResult.periodStart())
            || !periodEndExclusive.equals(outOfSampleResult.periodEndExclusive())) {
            throw new IllegalArgumentException("Out-of-sample result period differs from specification");
        }
        final List<WalkForwardWindow> windows = windows(
            periodStart, periodEndExclusive, specification.walkForwardCriteria().windowMonths()
        );

        final List<WalkForwardCandidateEvaluation> candidateResults = new ArrayList<>();
        for (OutOfSampleCandidateEvaluation outOfSample : outOfSampleResult.evaluations()) {
            candidateResults.add(evaluateCandidate(
                dataset,
                outOfSample.selectedCandidate(),
                windows,
                specification
            ));
        }
        return new WalkForwardEvaluationResult(
            outOfSampleResult.datasetId(),
            periodStart,
            periodEndExclusive,
            specification.walkForwardCriteria(),
            candidateResults
        );
    }

    private WalkForwardCandidateEvaluation evaluateCandidate(
        List<Candle> dataset,
        RankedCandidate candidate,
        List<WalkForwardWindow> windows,
        ParameterSearchSpecification specification
    ) {
        final List<WalkForwardWindowEvaluation> windowResults = new ArrayList<>(windows.size());
        for (WalkForwardWindow window : windows) {
            final long windowCandleCount = dataset.stream()
                .filter(candle -> !candle.getTime().isBefore(window.start())
                    && candle.getTime().isBefore(window.endExclusive()))
                .count();
            if (windowCandleCount < 2) {
                throw new IllegalArgumentException("Walk-forward window must contain at least two candles: " + window.index());
            }
            // 開始前の全履歴をウォームアップに使い、取引開始だけをウィンドウ先頭へ制限する。
            final List<Candle> replayCandles = dataset.stream()
                .filter(candle -> candle.getTime().isBefore(window.endExclusive()))
                .toList();
            final EmaCrossStrategy strategy = new EmaCrossStrategy(
                candleService,
                candidate.evaluation().parameters().toStrategyConfig()
            );
            final BacktestResult result = backTestService.run(
                replayCandles,
                strategy,
                specification.riskParameters(),
                window.start()
            );
            if (!result.assumptions().equals(specification.executionAssumptions())) {
                throw new IllegalStateException("Backtest execution assumptions differ from parameter-search specification");
            }
            final BacktestMetrics metrics = metricsCalculator.calculate(
                result, window.start(), window.endExclusive()
            );
            windowResults.add(new WalkForwardWindowEvaluation(window, result, metrics));
        }
        return summarize(candidate, windowResults, specification.walkForwardCriteria());
    }

    /** 市場条件の取り違え、重複、順序逆転を候補ごとの実行を始める前に検出する。 */
    private void validateDataset(List<Candle> dataset, ParameterSearchSpecification specification) {
        if (dataset.size() < 2) {
            throw new IllegalArgumentException("Walk-forward dataset must contain at least two candles");
        }
        LocalDateTime previousTime = null;
        for (Candle candle : dataset) {
            if (candle.getCurrencyPair() != specification.marketData().currencyPair()
                || candle.getTimeFrame() != specification.marketData().timeFrame()) {
                throw new IllegalArgumentException("Walk-forward candle does not match configured market data");
            }
            if (previousTime != null && !candle.getTime().isAfter(previousTime)) {
                throw new IllegalArgumentException("Walk-forward candles must be strictly ordered without duplicates");
            }
            previousTime = candle.getTime();
        }
    }

    private WalkForwardCandidateEvaluation summarize(
        RankedCandidate candidate,
        List<WalkForwardWindowEvaluation> windows,
        WalkForwardCriteria criteria
    ) {
        final long profitableWindows = windows.stream()
            .filter(window -> window.metrics().netProfit().signum() > 0)
            .count();
        final long positiveAverageRWindows = windows.stream()
            .filter(window -> window.metrics().averageR().filter(value -> value.signum() > 0).isPresent())
            .count();
        final BigDecimal profitableRate = rate(profitableWindows, windows.size());
        final BigDecimal positiveAverageRRate = rate(positiveAverageRWindows, windows.size());
        final List<WalkForwardRejectionReason> reasons = new ArrayList<>();
        if (windows.stream().anyMatch(window -> window.metrics().tradeCount() < criteria.minimumTradesPerWindow())) {
            reasons.add(WalkForwardRejectionReason.INSUFFICIENT_TRADES_IN_ONE_OR_MORE_WINDOWS);
        }
        if (profitableRate.compareTo(criteria.minimumProfitableWindowRate()) < 0) {
            reasons.add(WalkForwardRejectionReason.PROFITABLE_WINDOW_RATE_BELOW_MINIMUM);
        }
        if (positiveAverageRRate.compareTo(criteria.minimumPositiveAverageRWindowRate()) < 0) {
            reasons.add(WalkForwardRejectionReason.POSITIVE_AVERAGE_R_WINDOW_RATE_BELOW_MINIMUM);
        }
        return new WalkForwardCandidateEvaluation(
            candidate,
            windows,
            profitableRate,
            positiveAverageRRate,
            reasons.isEmpty(),
            reasons
        );
    }

    private List<WalkForwardWindow> windows(
        LocalDateTime start,
        LocalDateTime endExclusive,
        int windowMonths
    ) {
        final List<WalkForwardWindow> windows = new ArrayList<>();
        LocalDateTime windowStart = start;
        while (windowStart.isBefore(endExclusive)) {
            final LocalDateTime candidateEnd = windowStart.plusMonths(windowMonths);
            final LocalDateTime windowEnd = candidateEnd.isBefore(endExclusive) ? candidateEnd : endExclusive;
            windows.add(new WalkForwardWindow(windows.size() + 1, windowStart, windowEnd));
            windowStart = windowEnd;
        }
        return List.copyOf(windows);
    }

    private BigDecimal rate(long count, int total) {
        return BigDecimal.valueOf(count)
            .divide(BigDecimal.valueOf(total), RATE_SCALE, RoundingMode.HALF_UP)
            .stripTrailingZeros();
    }
}
