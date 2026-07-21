package com.takuro_tamura.autofx.parametersearch.outofsample;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestTrade;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * 固定候補のIn-sample・Out-of-sample指標比較と、Out-of-sample取引台帳をCSVへ保存する。
 * In-sample順位をそのまま出力し、Out-of-sample成績による再ランキングは行わない。
 */
public class OutOfSampleEvaluationWriter {

    public WrittenEvaluation write(
        Path outputDirectory,
        OutOfSampleEvaluationResult result,
        ParameterSearchSpecification specification
    ) {
        if (outputDirectory == null || result == null || specification == null) {
            throw new IllegalArgumentException("Output directory, evaluation result, and specification are required");
        }
        if (!result.datasetId().matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Dataset ID contains characters that are unsafe for a file name");
        }

        final String outputId = result.datasetId() + "_out_of_sample_evaluation";
        final Path summaryPath = outputDirectory.resolve(outputId + ".csv");
        final Path tradesPath = outputDirectory.resolve(outputId + ".trades.csv");
        try {
            Files.createDirectories(outputDirectory);
            if (Files.exists(summaryPath) || Files.exists(tradesPath)) {
                throw new IllegalStateException("Out-of-sample result is immutable and already exists: " + outputId);
            }
            Files.writeString(summaryPath, summaryCsv(result, specification), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
            Files.writeString(tradesPath, tradesCsv(result), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write Out-of-sample result: " + outputId, e);
        }
        return new WrittenEvaluation(summaryPath, tradesPath);
    }

    /** In-sampleとOut-of-sampleの主要指標、および単純差分を候補ごとに1行へまとめる。 */
    private String summaryCsv(
        OutOfSampleEvaluationResult result,
        ParameterSearchSpecification specification
    ) {
        final StringBuilder csv = new StringBuilder(
            "datasetId,inSampleFrom,inSampleTo,outOfSampleFrom,outOfSampleTo,inSampleRank,"
                + "emaShort,emaLong,rsiPeriod,macdFast,macdSlow,macdSignal,bbandsPeriod,bbandsMultiplier,"
                + "adxPeriod,adxThreshold,inTradeCount,outTradeCount,tradeCountDelta,inWinRate,outWinRate,"
                + "winRateDelta,inNetProfit,outNetProfit,netProfitDelta,inProfitFactor,outProfitFactor,"
                + "profitFactorDelta,inMaximumDrawdown,outMaximumDrawdown,maximumDrawdownDelta,"
                + "inAverageR,outAverageR,averageRDelta,inExposure,outExposure,spread,slippage,commission,"
                + "atrPeriod,stopMultiplier,profitMultiplier\n"
        );
        for (OutOfSampleCandidateEvaluation evaluation : result.evaluations()) {
            final StrategyParameterSet parameters = evaluation.selectedCandidate().evaluation().parameters();
            final BacktestMetrics inSample = evaluation.selectedCandidate().evaluation().metrics();
            final BacktestMetrics outOfSample = evaluation.metrics();
            csv.append(result.datasetId()).append(',')
                .append(specification.periods().inSampleFrom()).append(',')
                .append(specification.periods().inSampleTo()).append(',')
                .append(specification.periods().outOfSampleFrom()).append(',')
                .append(specification.periods().outOfSampleTo()).append(',')
                .append(evaluation.selectedCandidate().rank()).append(',')
                .append(parameters.emaShortPeriod()).append(',')
                .append(parameters.emaLongPeriod()).append(',')
                .append(parameters.rsiPeriod()).append(',')
                .append(parameters.macdFastPeriod()).append(',')
                .append(parameters.macdSlowPeriod()).append(',')
                .append(parameters.macdSignalPeriod()).append(',')
                .append(parameters.bBandsPeriod()).append(',')
                .append(parameters.bBandsMultiplier().toPlainString()).append(',')
                .append(parameters.adxPeriod()).append(',')
                .append(parameters.adxThreshold().toPlainString()).append(',')
                .append(inSample.tradeCount()).append(',')
                .append(outOfSample.tradeCount()).append(',')
                .append(outOfSample.tradeCount() - inSample.tradeCount()).append(',')
                .append(inSample.winRate().toPlainString()).append(',')
                .append(outOfSample.winRate().toPlainString()).append(',')
                .append(outOfSample.winRate().subtract(inSample.winRate()).toPlainString()).append(',')
                .append(inSample.netProfit().toPlainString()).append(',')
                .append(outOfSample.netProfit().toPlainString()).append(',')
                .append(outOfSample.netProfit().subtract(inSample.netProfit()).toPlainString()).append(',')
                .append(optional(inSample.profitFactor())).append(',')
                .append(optional(outOfSample.profitFactor())).append(',')
                .append(optionalDelta(outOfSample.profitFactor(), inSample.profitFactor())).append(',')
                .append(inSample.maximumDrawdown().toPlainString()).append(',')
                .append(outOfSample.maximumDrawdown().toPlainString()).append(',')
                .append(outOfSample.maximumDrawdown().subtract(inSample.maximumDrawdown()).toPlainString()).append(',')
                .append(optional(inSample.averageR())).append(',')
                .append(optional(outOfSample.averageR())).append(',')
                .append(optionalDelta(outOfSample.averageR(), inSample.averageR())).append(',')
                .append(inSample.exposure().toPlainString()).append(',')
                .append(outOfSample.exposure().toPlainString()).append(',')
                .append(specification.executionAssumptions().spread().toPlainString()).append(',')
                .append(specification.executionAssumptions().slippage().toPlainString()).append(',')
                .append(specification.executionAssumptions().commission().toPlainString()).append(',')
                .append(specification.riskParameters().atrPeriod()).append(',')
                .append(specification.riskParameters().stopMultiplier().toPlainString()).append(',')
                .append(specification.riskParameters().profitMultiplier().toPlainString()).append('\n');
        }
        return csv.toString();
    }

    /** Out-of-sampleで発生した取引だけを、元のIn-sample順位と結び付けて台帳へ出力する。 */
    private String tradesCsv(OutOfSampleEvaluationResult result) {
        final StringBuilder csv = new StringBuilder(
            "datasetId,inSampleRank,signalTime,fillTime,closeTime,side,size,fillPrice,closePrice,"
                + "entryAtr,stopPrice,takeProfitPrice,exitReason,profit\n"
        );
        for (OutOfSampleCandidateEvaluation evaluation : result.evaluations()) {
            for (BacktestTrade trade : evaluation.backtestResult().trades()) {
                final Order order = trade.order();
                csv.append(result.datasetId()).append(',')
                    .append(evaluation.selectedCandidate().rank()).append(',')
                    .append(trade.signalDatetime()).append(',')
                    .append(order.getFillDatetime()).append(',')
                    .append(order.getCloseDatetime()).append(',')
                    .append(order.getSide()).append(',')
                    .append(order.getSize()).append(',')
                    .append(order.getFillPrice().getValue().toPlainString()).append(',')
                    .append(order.getClosePrice().getValue().toPlainString()).append(',')
                    .append(order.getProtectionLevels().entryAtr().toPlainString()).append(',')
                    .append(order.getProtectionLevels().stopPrice().getValue().toPlainString()).append(',')
                    .append(order.getProtectionLevels().takeProfitPrice().getValue().toPlainString()).append(',')
                    .append(trade.exitReason()).append(',')
                    .append(order.calculateProfit().toPlainString()).append('\n');
            }
        }
        return csv.toString();
    }

    /** Optional指標は未定義をゼロと混同しないよう空欄へ変換する。 */
    private String optional(Optional<BigDecimal> value) {
        return value.map(BigDecimal::toPlainString).orElse("");
    }

    /** 両期間で指標が定義されている場合だけ、Out-of-sampleからIn-sampleを引いた差を返す。 */
    private String optionalDelta(Optional<BigDecimal> outOfSample, Optional<BigDecimal> inSample) {
        if (outOfSample.isEmpty() || inSample.isEmpty()) {
            return "";
        }
        return outOfSample.orElseThrow().subtract(inSample.orElseThrow()).toPlainString();
    }

    public record WrittenEvaluation(Path summaryPath, Path tradesPath) {
    }
}
