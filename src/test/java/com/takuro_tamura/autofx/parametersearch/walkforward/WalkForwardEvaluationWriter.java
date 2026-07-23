package com.takuro_tamura.autofx.parametersearch.walkforward;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestTrade;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

/** 候補別判定、ウィンドウ別指標、取引台帳を上書きしないCSVとして保存する。 */
public class WalkForwardEvaluationWriter {

    public WrittenWalkForwardEvaluation write(
        Path outputDirectory,
        WalkForwardEvaluationResult result,
        ParameterSearchSpecification specification
    ) {
        if (outputDirectory == null || result == null || specification == null) {
            throw new IllegalArgumentException("Output directory, walk-forward result, and specification are required");
        }
        if (!result.criteria().equals(specification.walkForwardCriteria())) {
            throw new IllegalArgumentException("Walk-forward criteria differ from parameter-search specification");
        }
        if (!result.periodStart().equals(specification.periods().outOfSampleFrom().atStartOfDay())
            || !result.periodEndExclusive().equals(
                specification.periods().outOfSampleTo().plusDays(1).atStartOfDay()
            )) {
            throw new IllegalArgumentException("Walk-forward result period differs from parameter-search specification");
        }
        if (!result.datasetId().matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Dataset ID contains characters that are unsafe for a file name");
        }

        final String outputId = result.datasetId() + "_walk_forward_evaluation";
        final Path summaryPath = outputDirectory.resolve(outputId + ".csv");
        final Path windowsPath = outputDirectory.resolve(outputId + ".windows.csv");
        final Path tradesPath = outputDirectory.resolve(outputId + ".trades.csv");
        try {
            Files.createDirectories(outputDirectory);
            if (Files.exists(summaryPath) || Files.exists(windowsPath) || Files.exists(tradesPath)) {
                throw new IllegalStateException("Walk-forward result is immutable and already exists: " + outputId);
            }
            Files.writeString(summaryPath, summaryCsv(result), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            Files.writeString(windowsPath, windowsCsv(result), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            Files.writeString(tradesPath, tradesCsv(result), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write walk-forward result: " + outputId, e);
        }
        return new WrittenWalkForwardEvaluation(summaryPath, windowsPath, tradesPath);
    }

    private String summaryCsv(WalkForwardEvaluationResult result) {
        final StringBuilder csv = new StringBuilder(
            "datasetId,inSampleRank,passed,rejectionReasons,emaShort,emaLong,rsiPeriod,macdFast,macdSlow,"
                + "macdSignal,bbandsPeriod,bbandsMultiplier,adxPeriod,adxThreshold,windowCount,"
                + "profitableWindowRate,positiveAverageRWindowRate,windowMonths,minimumTradesPerWindow,"
                + "minimumProfitableWindowRate,minimumPositiveAverageRWindowRate\n"
        );
        for (WalkForwardCandidateEvaluation candidate : result.candidates()) {
            final StrategyParameterSet parameters = candidate.selectedCandidate().evaluation().parameters();
            csv.append(result.datasetId()).append(',')
                .append(candidate.selectedCandidate().rank()).append(',')
                .append(candidate.passed()).append(',')
                .append(quote(candidate.rejectionReasons().stream().map(Enum::name).collect(Collectors.joining("|"))))
                .append(',').append(parameters.emaShortPeriod())
                .append(',').append(parameters.emaLongPeriod())
                .append(',').append(parameters.rsiPeriod())
                .append(',').append(parameters.macdFastPeriod())
                .append(',').append(parameters.macdSlowPeriod())
                .append(',').append(parameters.macdSignalPeriod())
                .append(',').append(parameters.bBandsPeriod())
                .append(',').append(parameters.bBandsMultiplier().toPlainString())
                .append(',').append(parameters.adxPeriod())
                .append(',').append(parameters.adxThreshold().toPlainString())
                .append(',').append(candidate.windows().size())
                .append(',').append(candidate.profitableWindowRate().toPlainString())
                .append(',').append(candidate.positiveAverageRWindowRate().toPlainString())
                .append(',').append(result.criteria().windowMonths())
                .append(',').append(result.criteria().minimumTradesPerWindow())
                .append(',').append(result.criteria().minimumProfitableWindowRate().toPlainString())
                .append(',').append(result.criteria().minimumPositiveAverageRWindowRate().toPlainString())
                .append('\n');
        }
        return csv.toString();
    }

    private String windowsCsv(WalkForwardEvaluationResult result) {
        final StringBuilder csv = new StringBuilder(
            "datasetId,inSampleRank,windowIndex,windowStart,windowEndExclusive,tradeCount,winRate,netProfit,"
                + "profitFactor,maximumDrawdown,maximumConsecutiveLosses,averageR,exposure\n"
        );
        for (WalkForwardCandidateEvaluation candidate : result.candidates()) {
            for (WalkForwardWindowEvaluation window : candidate.windows()) {
                final BacktestMetrics metrics = window.metrics();
                csv.append(result.datasetId()).append(',')
                    .append(candidate.selectedCandidate().rank()).append(',')
                    .append(window.window().index()).append(',')
                    .append(window.window().start()).append(',')
                    .append(window.window().endExclusive()).append(',')
                    .append(metrics.tradeCount()).append(',')
                    .append(metrics.winRate().toPlainString()).append(',')
                    .append(metrics.netProfit().toPlainString()).append(',')
                    .append(metrics.profitFactor().map(value -> value.toPlainString()).orElse("")).append(',')
                    .append(metrics.maximumDrawdown().toPlainString()).append(',')
                    .append(metrics.maximumConsecutiveLosses()).append(',')
                    .append(metrics.averageR().map(value -> value.toPlainString()).orElse("")).append(',')
                    .append(metrics.exposure().toPlainString()).append('\n');
            }
        }
        return csv.toString();
    }

    private String tradesCsv(WalkForwardEvaluationResult result) {
        final StringBuilder csv = new StringBuilder(
            "datasetId,inSampleRank,windowIndex,signalTime,fillTime,closeTime,side,size,fillPrice,closePrice,"
                + "entryAtr,stopPrice,takeProfitPrice,exitReason,profit\n"
        );
        for (WalkForwardCandidateEvaluation candidate : result.candidates()) {
            for (WalkForwardWindowEvaluation window : candidate.windows()) {
                for (BacktestTrade trade : window.backtestResult().trades()) {
                    final Order order = trade.order();
                    csv.append(result.datasetId()).append(',')
                        .append(candidate.selectedCandidate().rank()).append(',')
                        .append(window.window().index()).append(',')
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
        }
        return csv.toString();
    }

    private String quote(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    public record WrittenWalkForwardEvaluation(Path summaryPath, Path windowsPath, Path tradesPath) {
    }
}
