package com.takuro_tamura.autofx.parametersearch.execution;

import java.time.LocalDateTime;
import java.util.List;

/** データセットとIn-sample期間を固定した1回の探索実行結果。 */
public record InSampleParameterSearchResult(
    String datasetId,
    LocalDateTime periodStart,
    LocalDateTime periodEndExclusive,
    List<CandidateBacktestEvaluation> evaluations
) {
    public InSampleParameterSearchResult {
        if (datasetId == null || datasetId.isBlank() || periodStart == null || periodEndExclusive == null) {
            throw new IllegalArgumentException("Dataset identity and In-sample period are required");
        }
        if (!periodStart.isBefore(periodEndExclusive)) {
            throw new IllegalArgumentException("In-sample start must be before its exclusive end");
        }
        evaluations = List.copyOf(evaluations);
    }
}
