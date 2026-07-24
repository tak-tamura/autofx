package com.takuro_tamura.autofx.parametersearch.finalization;

import com.takuro_tamura.autofx.parametersearch.dataset.HistoricalDatasetMetadata;

import java.util.List;

/** データセット情報と全固定候補の最終判定を保持するPhase 9の結果。 */
public record ParameterSearchFinalResult(
    HistoricalDatasetMetadata datasetMetadata,
    List<FinalCandidateAssessment> candidates
) {
    public ParameterSearchFinalResult {
        if (datasetMetadata == null || candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Dataset metadata and final candidate assessments are required");
        }
        candidates = List.copyOf(candidates);
    }

    /** 人によるpaper運用レビューへ進められる候補だけを、In-sample順位のまま返す。 */
    public List<FinalCandidateAssessment> paperReviewCandidates() {
        return candidates.stream()
            .filter(candidate -> candidate.disposition() == CandidateDisposition.PAPER_REVIEW_CANDIDATE)
            .toList();
    }
}
