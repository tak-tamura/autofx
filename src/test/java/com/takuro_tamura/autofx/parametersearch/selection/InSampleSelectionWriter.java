package com.takuro_tamura.autofx.parametersearch.selection;

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

/**
 * In-sample順位表と候補別取引台帳を、レビューしやすい2つのCSVとして保存する。
 *
 * <p>順位表には全パラメータ、評価指標、約定・リスク仮定、選定基準を記録する。
 * 取引台帳には各候補のシグナルから決済までを記録する。同じデータセットIDの結果は上書きせず、
 * 過去の探索結果が後から別内容へ変わることを防ぐ。</p>
 */
public class InSampleSelectionWriter {

    /**
     * 選定結果を順位表CSVと取引台帳CSVへ保存する。
     *
     * @param outputDirectory 2つのCSVを作成するディレクトリ
     * @param selection In-sampleのランキングと選定結果
     * @param specification 探索時に固定した市場、約定、リスク、選定条件
     * @return 作成した順位表と取引台帳のパス
     * @throws IllegalStateException 同じデータセットIDの出力が既に存在する場合
     */
    public WrittenSelection write(
        Path outputDirectory,
        InSampleCandidateSelection selection,
        ParameterSearchSpecification specification
    ) {
        if (outputDirectory == null || selection == null || specification == null) {
            throw new IllegalArgumentException("Output directory, selection, and specification are required");
        }
        if (!selection.criteria().equals(specification.selectionCriteria())) {
            throw new IllegalArgumentException("Selection criteria differ from parameter-search specification");
        }
        if (!selection.datasetId().matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Dataset ID contains characters that are unsafe for a file name");
        }

        // データセットIDをファイル名へ含め、異なる入力データの結果を誤って比較・上書きしない。
        final String outputId = selection.datasetId() + "_in_sample_selection";
        final Path summaryPath = outputDirectory.resolve(outputId + ".csv");
        final Path tradesPath = outputDirectory.resolve(outputId + ".trades.csv");
        try {
            Files.createDirectories(outputDirectory);
            // 片方だけ存在する場合も不完全な過去出力とみなし、暗黙の再生成は行わない。
            if (Files.exists(summaryPath) || Files.exists(tradesPath)) {
                throw new IllegalStateException("Parameter-search result is immutable and already exists: " + outputId);
            }
            Files.writeString(summaryPath, summaryCsv(selection, specification), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
            Files.writeString(tradesPath, tradesCsv(selection), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write parameter-search result: " + outputId, e);
        }
        return new WrittenSelection(summaryPath, tradesPath);
    }

    /** 全候補を1行ずつ並べ、ランキングの根拠と再現条件を同じCSVへ展開する。 */
    private String summaryCsv(
        InSampleCandidateSelection selection,
        ParameterSearchSpecification specification
    ) {
        final StringBuilder csv = new StringBuilder(
            "datasetId,periodStart,periodEndExclusive,rank,eligible,selected,rejectionReasons,"
                + "emaShort,emaLong,rsiPeriod,macdFast,macdSlow,macdSignal,bbandsPeriod,bbandsMultiplier,"
                + "adxPeriod,adxThreshold,tradeCount,winRate,netProfit,profitFactor,maximumDrawdown,"
                + "maximumConsecutiveLosses,averageR,exposure,spread,slippage,commission,atrPeriod,"
                + "stopMultiplier,profitMultiplier,minimumTrades,minimumNetProfit,minimumProfitFactor,"
                + "minimumAverageR,maximumSelectedCandidates\n"
        );
        for (RankedCandidate ranked : selection.rankedCandidates()) {
            final StrategyParameterSet parameters = ranked.evaluation().parameters();
            final BacktestMetrics metrics = ranked.evaluation().metrics();
            // OptionalのPF・平均Rは、未定義であることをゼロと混同しないよう空欄で出力する。
            csv.append(selection.datasetId()).append(',')
                .append(selection.periodStart()).append(',')
                .append(selection.periodEndExclusive()).append(',')
                .append(ranked.rank()).append(',')
                .append(ranked.eligible()).append(',')
                .append(ranked.selected()).append(',')
                .append(quote(ranked.rejectionReasons().stream().map(Enum::name).collect(Collectors.joining("|"))))
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
                .append(',').append(metrics.tradeCount())
                .append(',').append(metrics.winRate().toPlainString())
                .append(',').append(metrics.netProfit().toPlainString())
                .append(',').append(metrics.profitFactor().map(value -> value.toPlainString()).orElse(""))
                .append(',').append(metrics.maximumDrawdown().toPlainString())
                .append(',').append(metrics.maximumConsecutiveLosses())
                .append(',').append(metrics.averageR().map(value -> value.toPlainString()).orElse(""))
                .append(',').append(metrics.exposure().toPlainString())
                .append(',').append(specification.executionAssumptions().spread().toPlainString())
                .append(',').append(specification.executionAssumptions().slippage().toPlainString())
                .append(',').append(specification.executionAssumptions().commission().toPlainString())
                .append(',').append(specification.riskParameters().atrPeriod())
                .append(',').append(specification.riskParameters().stopMultiplier().toPlainString())
                .append(',').append(specification.riskParameters().profitMultiplier().toPlainString())
                .append(',').append(selection.criteria().minimumTrades())
                .append(',').append(selection.criteria().minimumNetProfit().toPlainString())
                .append(',').append(selection.criteria().minimumProfitFactor().toPlainString())
                .append(',').append(selection.criteria().minimumAverageR().toPlainString())
                .append(',').append(selection.criteria().maximumSelectedCandidates())
                .append('\n');
        }
        return csv.toString();
    }

    /** 各候補の取引を時系列情報と保護水準を含む台帳形式へ展開する。 */
    private String tradesCsv(InSampleCandidateSelection selection) {
        final StringBuilder csv = new StringBuilder(
            "datasetId,rank,selected,signalTime,fillTime,closeTime,side,size,fillPrice,closePrice,"
                + "entryAtr,stopPrice,takeProfitPrice,exitReason,profit\n"
        );
        for (RankedCandidate ranked : selection.rankedCandidates()) {
            for (BacktestTrade trade : ranked.evaluation().backtestResult().trades()) {
                final Order order = trade.order();
                csv.append(selection.datasetId()).append(',')
                    .append(ranked.rank()).append(',')
                    .append(ranked.selected()).append(',')
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

    /** 棄却理由のような複合文字列をCSVの1フィールドとして安全に出力する。 */
    private String quote(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** 作成した順位表CSVと取引台帳CSVのパス。 */
    public record WrittenSelection(Path summaryPath, Path tradesPath) {
    }
}
