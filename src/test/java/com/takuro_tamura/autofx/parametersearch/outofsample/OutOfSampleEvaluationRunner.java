package com.takuro_tamura.autofx.parametersearch.outofsample;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetricsCalculator;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.service.BackTestService;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.strategy.EmaCrossStrategy;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;
import com.takuro_tamura.autofx.parametersearch.selection.InSampleCandidateSelection;
import com.takuro_tamura.autofx.parametersearch.selection.RankedCandidate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 6で選定済みの候補だけを、未使用のOut-of-sample期間へ適用する。
 * 選定候補とその順序は変更せず、将来データに相当する期間での劣化や安定性を測定する。
 */
public class OutOfSampleEvaluationRunner {
    private final CandleService candleService;
    private final BackTestService backTestService;
    private final BacktestMetricsCalculator metricsCalculator;

    public OutOfSampleEvaluationRunner(
        CandleService candleService,
        BackTestService backTestService,
        BacktestMetricsCalculator metricsCalculator
    ) {
        this.candleService = candleService;
        this.backTestService = backTestService;
        this.metricsCalculator = metricsCalculator;
    }

    /**
     * 固定済み候補をIn-sample順位のままOut-of-sample期間で評価する。
     *
     * @param dataset Phase 2で取得・検証した全期間のローソク足
     * @param selection Phase 6でOut-of-sample対象を固定した結果
     * @param specification 市場、期間、約定、リスク条件
     */
    public OutOfSampleEvaluationResult run(
        List<Candle> dataset,
        InSampleCandidateSelection selection,
        ParameterSearchSpecification specification
    ) {
        if (dataset == null || selection == null || specification == null) {
            throw new IllegalArgumentException("Dataset, In-sample selection, and specification are required");
        }
        if (dataset.stream().anyMatch(candle -> candle == null || candle.getTime() == null)) {
            throw new IllegalArgumentException("Dataset must not contain null candles or timestamps");
        }
        final List<RankedCandidate> selectedCandidates = selection.selectedCandidates();
        if (selectedCandidates.isEmpty()) {
            throw new IllegalArgumentException("At least one In-sample candidate must be selected");
        }

        final LocalDateTime periodStart = specification.periods().outOfSampleFrom().atStartOfDay();
        // 終了日を含め、翌日0時を排他的境界としてバックテスト期間と集計期間を一致させる。
        final LocalDateTime periodEndExclusive = specification.periods().outOfSampleTo().plusDays(1).atStartOfDay();
        final List<Candle> outOfSampleCandles = dataset.stream()
            .filter(candle -> !candle.getTime().isBefore(periodStart)
                && candle.getTime().isBefore(periodEndExclusive))
            .toList();
        validateCandles(outOfSampleCandles, specification);
        // OOS開始前の足も指標のウォームアップに使用するが、BackTestService側で期間前シグナルを禁止する。
        final List<Candle> replayCandles = dataset.stream()
            .filter(candle -> candle.getTime().isBefore(periodEndExclusive))
            .toList();
        validateCandles(replayCandles, specification);

        final List<OutOfSampleCandidateEvaluation> evaluations = new ArrayList<>(selectedCandidates.size());
        for (RankedCandidate candidate : selectedCandidates) {
            // 候補ごとに戦略を作り直し、In-sample実行や他候補の計算状態を共有しない。
            final EmaCrossStrategy strategy = new EmaCrossStrategy(
                candleService,
                candidate.evaluation().parameters().toStrategyConfig()
            );
            final BacktestResult result = backTestService.run(
                replayCandles,
                strategy,
                specification.riskParameters(),
                periodStart
            );
            if (!result.assumptions().equals(specification.executionAssumptions())) {
                throw new IllegalStateException("Backtest execution assumptions differ from parameter-search specification");
            }
            final BacktestMetrics metrics = metricsCalculator.calculate(result, periodStart, periodEndExclusive);
            evaluations.add(new OutOfSampleCandidateEvaluation(candidate, result, metrics));
        }

        return new OutOfSampleEvaluationResult(
            selection.datasetId(),
            periodStart,
            periodEndExclusive,
            evaluations
        );
    }

    private void validateCandles(
        List<Candle> candles,
        ParameterSearchSpecification specification
    ) {
        if (candles.size() < 2) {
            throw new IllegalArgumentException("Out-of-sample dataset must contain at least two candles");
        }
        LocalDateTime previousTime = null;
        for (Candle candle : candles) {
            if (candle.getCurrencyPair() != specification.marketData().currencyPair()
                || candle.getTimeFrame() != specification.marketData().timeFrame()) {
                throw new IllegalArgumentException("Out-of-sample candle does not match configured market data");
            }
            if (previousTime != null && !candle.getTime().isAfter(previousTime)) {
                throw new IllegalArgumentException("Out-of-sample candles must be strictly ordered without duplicates");
            }
            previousTime = candle.getTime();
        }
    }
}
