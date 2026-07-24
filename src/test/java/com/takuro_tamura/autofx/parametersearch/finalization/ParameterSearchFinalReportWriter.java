package com.takuro_tamura.autofx.parametersearch.finalization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.domain.backtest.BacktestRiskParameters;
import com.takuro_tamura.autofx.domain.strategy.EmaCrossStrategy;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;
import com.takuro_tamura.autofx.parametersearch.config.WalkForwardCriteria;
import com.takuro_tamura.autofx.parametersearch.dataset.HistoricalDatasetMetadata;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全候補の最終レビューCSVと、paperレビュー候補だけを含む安全なJSON manifestを保存する。
 * manifestは常に手動レビュー必須・ライブ取引不許可とし、設定適用処理は持たない。
 */
public class ParameterSearchFinalReportWriter {
    private static final int MANIFEST_SCHEMA_VERSION = 1;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        // BigDecimalをJSON数値のまま保ちつつ、2E+1ではなく20のような通常表記で保存する。
        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    private final Clock clock;

    public ParameterSearchFinalReportWriter(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required");
        }
        this.clock = clock;
    }

    public WrittenFinalReport write(
        Path outputDirectory,
        ParameterSearchFinalResult result,
        ParameterSearchSpecification specification
    ) {
        if (outputDirectory == null || result == null || specification == null) {
            throw new IllegalArgumentException("Output directory, final result, and specification are required");
        }
        final String datasetId = result.datasetMetadata().datasetId();
        if (!datasetId.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Dataset ID contains characters that are unsafe for a file name");
        }
        final Path reviewPath = outputDirectory.resolve(datasetId + "_final_review.csv");
        final Path manifestPath = outputDirectory.resolve(datasetId + "_paper_candidates.json");
        try {
            Files.createDirectories(outputDirectory);
            if (Files.exists(reviewPath) || Files.exists(manifestPath)) {
                throw new IllegalStateException("Final parameter-search report is immutable and already exists: " + datasetId);
            }
            Files.writeString(reviewPath, reviewCsv(result, specification), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(
                manifestPath.toFile(), manifest(result, specification)
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write final parameter-search report: " + datasetId, e);
        }
        return new WrittenFinalReport(reviewPath, manifestPath);
    }

    private String reviewCsv(
        ParameterSearchFinalResult result,
        ParameterSearchSpecification specification
    ) {
        final StringBuilder csv = new StringBuilder(
            "datasetId,datasetSha256,inSampleRank,disposition,rejectionReasons,emaShort,emaLong,rsiPeriod,"
                + "macdFast,macdSlow,macdSignal,bbandsPeriod,bbandsMultiplier,adxPeriod,adxThreshold,"
                + "inTradeCount,inNetProfit,inProfitFactor,inMaximumDrawdown,inAverageR,outTradeCount,"
                + "outNetProfit,outProfitFactor,outMaximumDrawdown,outAverageR,profitableWindowRate,"
                + "positiveAverageRWindowRate,spread,slippage,commission,atrPeriod,stopMultiplier,profitMultiplier\n"
        );
        for (FinalCandidateAssessment candidate : result.candidates()) {
            final StrategyParameterSet parameters = candidate.parameters();
            csv.append(result.datasetMetadata().datasetId()).append(',')
                .append(result.datasetMetadata().sha256()).append(',')
                .append(candidate.inSampleRank()).append(',')
                .append(candidate.disposition()).append(',')
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
                .append(',').append(candidate.inSampleMetrics().tradeCount())
                .append(',').append(candidate.inSampleMetrics().netProfit().toPlainString())
                .append(',').append(optional(candidate.inSampleMetrics().profitFactor().orElse(null)))
                .append(',').append(candidate.inSampleMetrics().maximumDrawdown().toPlainString())
                .append(',').append(optional(candidate.inSampleMetrics().averageR().orElse(null)))
                .append(',').append(candidate.outOfSampleMetrics().tradeCount())
                .append(',').append(candidate.outOfSampleMetrics().netProfit().toPlainString())
                .append(',').append(optional(candidate.outOfSampleMetrics().profitFactor().orElse(null)))
                .append(',').append(candidate.outOfSampleMetrics().maximumDrawdown().toPlainString())
                .append(',').append(optional(candidate.outOfSampleMetrics().averageR().orElse(null)))
                .append(',').append(candidate.profitableWindowRate().toPlainString())
                .append(',').append(candidate.positiveAverageRWindowRate().toPlainString())
                .append(',').append(specification.executionAssumptions().spread().toPlainString())
                .append(',').append(specification.executionAssumptions().slippage().toPlainString())
                .append(',').append(specification.executionAssumptions().commission().toPlainString())
                .append(',').append(specification.riskParameters().atrPeriod())
                .append(',').append(specification.riskParameters().stopMultiplier().toPlainString())
                .append(',').append(specification.riskParameters().profitMultiplier().toPlainString())
                .append('\n');
        }
        return csv.toString();
    }

    private PaperCandidateManifest manifest(
        ParameterSearchFinalResult result,
        ParameterSearchSpecification specification
    ) {
        final List<PaperCandidate> candidates = result.paperReviewCandidates().stream()
            .map(candidate -> new PaperCandidate(
                candidate.inSampleRank(),
                candidate.parameters(),
                MetricSnapshot.from(candidate.inSampleMetrics()),
                MetricSnapshot.from(candidate.outOfSampleMetrics()),
                candidate.profitableWindowRate(),
                candidate.positiveAverageRWindowRate()
            ))
            .toList();
        return new PaperCandidateManifest(
            MANIFEST_SCHEMA_VERSION,
            clock.instant(),
            true,
            false,
            EmaCrossStrategy.class.getName(),
            result.datasetMetadata(),
            specification.executionAssumptions(),
            specification.riskParameters(),
            specification.walkForwardCriteria(),
            candidates
        );
    }

    private String optional(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String quote(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    public record WrittenFinalReport(Path reviewPath, Path manifestPath) {
    }

    private record PaperCandidateManifest(
        int schemaVersion,
        Instant generatedAt,
        boolean manualReviewRequired,
        boolean liveTradingAllowed,
        String strategyClass,
        HistoricalDatasetMetadata dataset,
        BacktestAssumptions executionAssumptions,
        BacktestRiskParameters riskParameters,
        WalkForwardCriteria walkForwardCriteria,
        List<PaperCandidate> candidates
    ) {
    }

    private record PaperCandidate(
        int inSampleRank,
        StrategyParameterSet parameters,
        MetricSnapshot inSampleMetrics,
        MetricSnapshot outOfSampleMetrics,
        BigDecimal profitableWindowRate,
        BigDecimal positiveAverageRWindowRate
    ) {
    }

    private record MetricSnapshot(
        int tradeCount,
        BigDecimal winRate,
        BigDecimal netProfit,
        BigDecimal profitFactor,
        BigDecimal maximumDrawdown,
        int maximumConsecutiveLosses,
        BigDecimal averageR,
        BigDecimal exposure,
        BigDecimal transactionCosts
    ) {
        private static MetricSnapshot from(BacktestMetrics metrics) {
            return new MetricSnapshot(
                metrics.tradeCount(),
                metrics.winRate(),
                metrics.netProfit(),
                metrics.profitFactor().orElse(null),
                metrics.maximumDrawdown(),
                metrics.maximumConsecutiveLosses(),
                metrics.averageR().orElse(null),
                metrics.exposure(),
                metrics.totalTransactionCosts()
            );
        }
    }
}
