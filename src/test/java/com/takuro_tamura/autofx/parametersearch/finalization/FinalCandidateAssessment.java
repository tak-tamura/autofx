package com.takuro_tamura.autofx.parametersearch.finalization;

import com.takuro_tamura.autofx.domain.backtest.BacktestMetrics;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;
import com.takuro_tamura.autofx.parametersearch.walkforward.WalkForwardRejectionReason;

import java.math.BigDecimal;
import java.util.List;

/** 1候補のIS・OOS・期間安定性をまとめた最終レビュー行。 */
public record FinalCandidateAssessment(
    int inSampleRank,
    StrategyParameterSet parameters,
    BacktestMetrics inSampleMetrics,
    BacktestMetrics outOfSampleMetrics,
    BigDecimal profitableWindowRate,
    BigDecimal positiveAverageRWindowRate,
    CandidateDisposition disposition,
    List<WalkForwardRejectionReason> rejectionReasons
) {
    public FinalCandidateAssessment {
        if (inSampleRank <= 0 || parameters == null || inSampleMetrics == null || outOfSampleMetrics == null
            || profitableWindowRate == null || positiveAverageRWindowRate == null
            || disposition == null || rejectionReasons == null) {
            throw new IllegalArgumentException("Complete final candidate assessment is required");
        }
        rejectionReasons = List.copyOf(rejectionReasons);
        if ((disposition == CandidateDisposition.PAPER_REVIEW_CANDIDATE) != rejectionReasons.isEmpty()) {
            throw new IllegalArgumentException("Candidate disposition and rejection reasons are inconsistent");
        }
    }
}
