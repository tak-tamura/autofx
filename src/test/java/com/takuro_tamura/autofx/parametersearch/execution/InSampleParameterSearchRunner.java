package com.takuro_tamura.autofx.parametersearch.execution;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetricsCalculator;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.service.BackTestService;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.strategy.EmaCrossStrategy;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterCandidateGenerator;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 固定データセットのIn-sample部分に全候補を適用し、同じ約定条件で比較可能な結果を作る。
 * 候補の順位付けやOut-of-sample評価は行わず、探索結果をそのまま保持する。
 */
public class InSampleParameterSearchRunner {
    private final CandleService candleService;
    private final BackTestService backTestService;
    private final BacktestMetricsCalculator metricsCalculator;
    private final StrategyParameterCandidateGenerator candidateGenerator;

    public InSampleParameterSearchRunner(
        CandleService candleService,
        BackTestService backTestService,
        BacktestMetricsCalculator metricsCalculator,
        StrategyParameterCandidateGenerator candidateGenerator
    ) {
        this.candleService = candleService;
        this.backTestService = backTestService;
        this.metricsCalculator = metricsCalculator;
        this.candidateGenerator = candidateGenerator;
    }

    public InSampleParameterSearchResult run(
        String datasetId,
        List<Candle> dataset,
        ParameterSearchSpecification specification
    ) {
        if (datasetId == null || datasetId.isBlank() || dataset == null || specification == null) {
            throw new IllegalArgumentException("Dataset identity, candles, and parameter-search specification are required");
        }
        if (dataset.stream().anyMatch(candle -> candle == null || candle.getTime() == null)) {
            throw new IllegalArgumentException("Dataset must not contain null candles or timestamps");
        }

        final LocalDateTime periodStart = specification.periods().inSampleFrom().atStartOfDay();
        // 終了日は含めるため、翌日0時を排他的な境界として扱う。
        final LocalDateTime periodEndExclusive = specification.periods().inSampleTo().plusDays(1).atStartOfDay();
        final List<Candle> inSampleCandles = dataset.stream()
            .filter(candle -> !candle.getTime().isBefore(periodStart)
                && candle.getTime().isBefore(periodEndExclusive))
            .toList();
        validateCandles(inSampleCandles, specification);

        final List<StrategyParameterSet> candidates = candidateGenerator.generate(
            specification.strategySearchSpace()
        );
        final List<CandidateBacktestEvaluation> evaluations = new ArrayList<>(candidates.size());
        for (StrategyParameterSet candidate : candidates) {
            // 本番と共通の戦略・バックテスト実装を、候補ごとに新規生成して状態を共有しない。
            final EmaCrossStrategy strategy = new EmaCrossStrategy(candleService, candidate.toStrategyConfig());
            final BacktestResult result = backTestService.run(
                inSampleCandles,
                strategy,
                specification.riskParameters()
            );
            if (!result.assumptions().equals(specification.executionAssumptions())) {
                throw new IllegalStateException("Backtest execution assumptions differ from parameter-search specification");
            }
            final BacktestMetrics metrics = metricsCalculator.calculate(result, periodStart, periodEndExclusive);
            evaluations.add(new CandidateBacktestEvaluation(candidate, result, metrics));
        }

        return new InSampleParameterSearchResult(
            datasetId,
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
            throw new IllegalArgumentException("In-sample dataset must contain at least two candles");
        }
        LocalDateTime previousTime = null;
        for (Candle candle : candles) {
            if (candle.getTime() == null
                || candle.getCurrencyPair() != specification.marketData().currencyPair()
                || candle.getTimeFrame() != specification.marketData().timeFrame()) {
                throw new IllegalArgumentException("In-sample candle does not match configured market data");
            }
            if (previousTime != null && !candle.getTime().isAfter(previousTime)) {
                throw new IllegalArgumentException("In-sample candles must be strictly ordered without duplicates");
            }
            previousTime = candle.getTime();
        }
    }
}
