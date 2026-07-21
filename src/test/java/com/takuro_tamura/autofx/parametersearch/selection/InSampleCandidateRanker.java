package com.takuro_tamura.autofx.parametersearch.selection;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.parametersearch.config.CandidateSelectionCriteria;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;
import com.takuro_tamura.autofx.parametersearch.execution.CandidateBacktestEvaluation;
import com.takuro_tamura.autofx.parametersearch.execution.InSampleParameterSearchResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * In-sample指標だけを使用して候補を適格判定し、再現可能な順序で並べる。
 * 総利益だけで改善を判断せず、平均R、PF、最大DD、取引数を順に比較する。
 */
public class InSampleCandidateRanker {

    /**
     * 全候補へ適格判定と順位を付け、上限件数までをOut-of-sample評価対象として固定する。
     *
     * <p>このメソッドが参照するのは渡されたIn-sample結果だけであり、Out-of-sample成績による
     * 候補の再選定を防ぐ。戻り値には不適格候補も含め、棄却理由を後から確認できるようにする。</p>
     *
     * @param searchResult Phase 5で生成したIn-sample候補別バックテスト結果
     * @param criteria 候補の適格判定と選定件数に使用する固定基準
     * @return 全候補の順位、適格状態、選定状態を保持する結果
     */
    public InSampleCandidateSelection rank(
        InSampleParameterSearchResult searchResult,
        CandidateSelectionCriteria criteria
    ) {
        if (searchResult == null || criteria == null) {
            throw new IllegalArgumentException("In-sample search result and selection criteria are required");
        }

        // ソート前に全候補の棄却理由を確定し、ランキング順によって適格判定が変わらないようにする。
        final List<EvaluatedCandidate> evaluated = searchResult.evaluations().stream()
            .map(evaluation -> new EvaluatedCandidate(evaluation, rejectionReasons(evaluation.metrics(), criteria)))
            .sorted(candidateComparator())
            .toList();

        // 適格候補だけを順位順に上限件数まで選定する。不適格候補も監査用に順位表へ残す。
        int selectedCount = 0;
        final List<RankedCandidate> ranked = new ArrayList<>(evaluated.size());
        for (int index = 0; index < evaluated.size(); index++) {
            final EvaluatedCandidate candidate = evaluated.get(index);
            final boolean eligible = candidate.rejectionReasons().isEmpty();
            final boolean selected = eligible && selectedCount < criteria.maximumSelectedCandidates();
            if (selected) {
                selectedCount++;
            }
            ranked.add(new RankedCandidate(
                index + 1,
                eligible,
                selected,
                candidate.rejectionReasons(),
                candidate.evaluation()
            ));
        }

        return new InSampleCandidateSelection(
            searchResult.datasetId(),
            searchResult.periodStart(),
            searchResult.periodEndExclusive(),
            criteria,
            ranked
        );
    }

    /** 各最低基準を独立に評価し、違反した理由を省略せず返す。 */
    private List<CandidateRejectionReason> rejectionReasons(
        BacktestMetrics metrics,
        CandidateSelectionCriteria criteria
    ) {
        final List<CandidateRejectionReason> reasons = new ArrayList<>();
        if (metrics.tradeCount() < criteria.minimumTrades()) {
            reasons.add(CandidateRejectionReason.INSUFFICIENT_TRADES);
        }
        if (metrics.netProfit().compareTo(criteria.minimumNetProfit()) < 0) {
            reasons.add(CandidateRejectionReason.NET_PROFIT_BELOW_MINIMUM);
        }
        // 損失取引がない場合、PFは数学的に未定義だが最低PFを下回ったとは扱わない。
        if (metrics.profitFactor().isPresent()
            && metrics.profitFactor().orElseThrow().compareTo(criteria.minimumProfitFactor()) < 0) {
            reasons.add(CandidateRejectionReason.PROFIT_FACTOR_BELOW_MINIMUM);
        }
        if (metrics.averageR().isEmpty()
            || metrics.averageR().orElseThrow().compareTo(criteria.minimumAverageR()) < 0) {
            reasons.add(CandidateRejectionReason.AVERAGE_R_BELOW_MINIMUM);
        }
        return List.copyOf(reasons);
    }

    /**
     * 候補の比較順を一か所で定義する。
     * 適格候補を先頭に置いた後、平均R、PFを降順、最大DDを昇順、取引数と純利益を降順で比較する。
     * 全指標が同じ場合はパラメータキーを使い、入力リストの順序に依存しない結果にする。
     */
    private Comparator<EvaluatedCandidate> candidateComparator() {
        return Comparator
            .comparing(EvaluatedCandidate::eligible).reversed()
            .thenComparing((EvaluatedCandidate value) -> averageR(value.metrics()), Comparator.reverseOrder())
            .thenComparing((EvaluatedCandidate value) -> profitFactor(value.metrics()), Comparator.reverseOrder())
            .thenComparing(value -> value.metrics().maximumDrawdown())
            .thenComparing((EvaluatedCandidate value) -> value.metrics().tradeCount(), Comparator.reverseOrder())
            .thenComparing((EvaluatedCandidate value) -> value.metrics().netProfit(), Comparator.reverseOrder())
            .thenComparing(value -> parameterKey(value.evaluation().parameters()));
    }

    /** 平均Rを算出できない候補を、算出可能な候補より後ろへ並べるための比較値へ変換する。 */
    private BigDecimal averageR(BacktestMetrics metrics) {
        return metrics.averageR().orElse(BigDecimal.valueOf(Long.MIN_VALUE));
    }

    /** PF未定義のケースを、損失なしの利益候補と取引実績なしの候補に分けて比較値へ変換する。 */
    private BigDecimal profitFactor(BacktestMetrics metrics) {
        if (metrics.profitFactor().isPresent()) {
            return metrics.profitFactor().orElseThrow();
        }
        // 利益があり損失がない候補は無限大相当、取引がない候補は最低値として並べる。
        return metrics.grossProfit().signum() > 0
            ? BigDecimal.valueOf(Long.MAX_VALUE)
            : BigDecimal.valueOf(Long.MIN_VALUE);
    }

    /** 全評価指標が同値でも順位を再現できるよう、全戦略パラメータから安定した比較キーを作る。 */
    private String parameterKey(StrategyParameterSet value) {
        return String.join("-",
            Integer.toString(value.emaShortPeriod()),
            Integer.toString(value.emaLongPeriod()),
            Integer.toString(value.rsiPeriod()),
            Integer.toString(value.macdFastPeriod()),
            Integer.toString(value.macdSlowPeriod()),
            Integer.toString(value.macdSignalPeriod()),
            Integer.toString(value.bBandsPeriod()),
            value.bBandsMultiplier().toPlainString(),
            Integer.toString(value.adxPeriod()),
            value.adxThreshold().toPlainString()
        );
    }

    /** ソート中に適格判定を再計算しないよう、評価結果と棄却理由を一時的にまとめる内部表現。 */
    private record EvaluatedCandidate(
        CandidateBacktestEvaluation evaluation,
        List<CandidateRejectionReason> rejectionReasons
    ) {
        private boolean eligible() {
            return rejectionReasons.isEmpty();
        }

        private BacktestMetrics metrics() {
            return evaluation.metrics();
        }
    }
}
