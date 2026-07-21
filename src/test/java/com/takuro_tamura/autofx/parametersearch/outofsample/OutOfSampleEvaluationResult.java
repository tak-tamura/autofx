package com.takuro_tamura.autofx.parametersearch.outofsample;

import java.time.LocalDateTime;
import java.util.List;

/** 同一データセットのOut-of-sample期間で、固定候補だけを評価した結果。 */
public record OutOfSampleEvaluationResult(
    String datasetId,
    LocalDateTime periodStart,
    LocalDateTime periodEndExclusive,
    List<OutOfSampleCandidateEvaluation> evaluations
) {
    public OutOfSampleEvaluationResult {
        if (datasetId == null || datasetId.isBlank() || periodStart == null || periodEndExclusive == null
            || evaluations == null) {
            throw new IllegalArgumentException("Dataset, period, and Out-of-sample evaluations are required");
        }
        if (!periodStart.isBefore(periodEndExclusive)) {
            throw new IllegalArgumentException("Out-of-sample start must be before its exclusive end");
        }
        evaluations = List.copyOf(evaluations);
    }
}
