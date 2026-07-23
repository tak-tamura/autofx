package com.takuro_tamura.autofx.parametersearch.walkforward;

import com.takuro_tamura.autofx.parametersearch.config.WalkForwardCriteria;

import java.time.LocalDateTime;
import java.util.List;

/** 全固定候補に対するウォークフォワード評価結果。合格はpaper運用レビュー候補を意味する。 */
public record WalkForwardEvaluationResult(
    String datasetId,
    LocalDateTime periodStart,
    LocalDateTime periodEndExclusive,
    WalkForwardCriteria criteria,
    List<WalkForwardCandidateEvaluation> candidates
) {
    public WalkForwardEvaluationResult {
        if (datasetId == null || datasetId.isBlank() || periodStart == null || periodEndExclusive == null
            || criteria == null || candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Dataset, period, criteria, and candidates are required");
        }
        if (!periodStart.isBefore(periodEndExclusive)) {
            throw new IllegalArgumentException("Walk-forward start must be before its exclusive end");
        }
        candidates = List.copyOf(candidates);
    }
}
