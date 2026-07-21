package com.takuro_tamura.autofx.parametersearch.selection;

import com.takuro_tamura.autofx.parametersearch.config.CandidateSelectionCriteria;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Out-of-sample評価前に固定するIn-sample候補選定結果。
 * データセット、評価期間、選定基準、全候補の順位をまとめ、どの条件で候補を選んだかを再現可能にする。
 */
public record InSampleCandidateSelection(
    String datasetId,
    LocalDateTime periodStart,
    LocalDateTime periodEndExclusive,
    CandidateSelectionCriteria criteria,
    List<RankedCandidate> rankedCandidates
) {
    /** 呼び出し側の変更で選定結果が変化しないよう、順位一覧を不変コピーとして保持する。 */
    public InSampleCandidateSelection {
        if (datasetId == null || datasetId.isBlank() || periodStart == null || periodEndExclusive == null
            || criteria == null || rankedCandidates == null) {
            throw new IllegalArgumentException("Dataset, period, criteria, and ranked candidates are required");
        }
        rankedCandidates = List.copyOf(rankedCandidates);
    }

    /**
     * 適格候補のうち、Out-of-sample評価対象として選定された候補だけを順位順に返す。
     *
     * @return {@link RankedCandidate#selected()}がtrueの候補
     */
    public List<RankedCandidate> selectedCandidates() {
        return rankedCandidates.stream().filter(RankedCandidate::selected).toList();
    }
}
