package com.takuro_tamura.autofx.parametersearch.walkforward;

import com.takuro_tamura.autofx.parametersearch.selection.RankedCandidate;

import java.math.BigDecimal;
import java.util.List;

/** 固定候補の全ウィンドウ結果と、paper運用レビューへ進めるための安定性判定。 */
public record WalkForwardCandidateEvaluation(
    RankedCandidate selectedCandidate,
    List<WalkForwardWindowEvaluation> windows,
    BigDecimal profitableWindowRate,
    BigDecimal positiveAverageRWindowRate,
    boolean passed,
    List<WalkForwardRejectionReason> rejectionReasons
) {
    public WalkForwardCandidateEvaluation {
        if (selectedCandidate == null || !selectedCandidate.selected() || windows == null || windows.isEmpty()
            || profitableWindowRate == null || positiveAverageRWindowRate == null || rejectionReasons == null) {
            throw new IllegalArgumentException("Selected candidate, windows, rates, and rejection reasons are required");
        }
        windows = List.copyOf(windows);
        rejectionReasons = List.copyOf(rejectionReasons);
        if (passed == !rejectionReasons.isEmpty()) {
            throw new IllegalArgumentException("Walk-forward pass state and rejection reasons are inconsistent");
        }
    }
}
