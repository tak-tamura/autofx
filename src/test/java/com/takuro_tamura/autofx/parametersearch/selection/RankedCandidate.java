package com.takuro_tamura.autofx.parametersearch.selection;

import com.takuro_tamura.autofx.parametersearch.execution.CandidateBacktestEvaluation;

import java.util.List;

/**
 * In-sample指標による順位と適格判定を付与した候補。
 * {@code eligible}は最低基準の通過、{@code selected}はその中から選定上限内に入ったことを表す。
 */
public record RankedCandidate(
    int rank,
    boolean eligible,
    boolean selected,
    List<CandidateRejectionReason> rejectionReasons,
    CandidateBacktestEvaluation evaluation
) {
    /** 順位、適格状態、棄却理由の矛盾を生成時に拒否する。 */
    public RankedCandidate {
        if (rank <= 0 || evaluation == null || rejectionReasons == null) {
            throw new IllegalArgumentException("Positive rank, evaluation, and rejection reasons are required");
        }
        rejectionReasons = List.copyOf(rejectionReasons);
        if (eligible == !rejectionReasons.isEmpty()) {
            throw new IllegalArgumentException("Eligibility and rejection reasons are inconsistent");
        }
        if (selected && !eligible) {
            throw new IllegalArgumentException("An ineligible candidate cannot be selected");
        }
    }
}
