package com.takuro_tamura.autofx.parametersearch.finalization;

import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;
import com.takuro_tamura.autofx.parametersearch.dataset.HistoricalDatasetMetadata;
import com.takuro_tamura.autofx.parametersearch.outofsample.OutOfSampleCandidateEvaluation;
import com.takuro_tamura.autofx.parametersearch.outofsample.OutOfSampleEvaluationResult;
import com.takuro_tamura.autofx.parametersearch.walkforward.WalkForwardCandidateEvaluation;
import com.takuro_tamura.autofx.parametersearch.walkforward.WalkForwardEvaluationResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Phase 7とPhase 8の結果を照合し、候補をpaperレビュー対象または棄却として確定する。
 * OOS指標による再ランキングや、DB・paper/live設定の更新は行わない。
 */
public class ParameterSearchFinalizer {

    public ParameterSearchFinalResult assemble(
        HistoricalDatasetMetadata metadata,
        OutOfSampleEvaluationResult outOfSampleResult,
        WalkForwardEvaluationResult walkForwardResult,
        ParameterSearchSpecification specification
    ) {
        if (metadata == null || outOfSampleResult == null || walkForwardResult == null || specification == null) {
            throw new IllegalArgumentException("Dataset, OOS, walk-forward, and specification are required");
        }
        validateDatasetIdentity(metadata, outOfSampleResult, walkForwardResult, specification);
        if (outOfSampleResult.evaluations().size() != walkForwardResult.candidates().size()) {
            throw new IllegalArgumentException("OOS and walk-forward candidate counts differ");
        }
        validateCandidateRanks(outOfSampleResult, walkForwardResult);

        final List<FinalCandidateAssessment> assessments = new ArrayList<>();
        for (OutOfSampleCandidateEvaluation outOfSample : outOfSampleResult.evaluations()) {
            final int rank = outOfSample.selectedCandidate().rank();
            final WalkForwardCandidateEvaluation walkForward = walkForwardResult.candidates().stream()
                .filter(candidate -> candidate.selectedCandidate().rank() == rank)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Walk-forward result is missing In-sample rank " + rank
                ));
            if (!outOfSample.selectedCandidate().evaluation().parameters()
                .equals(walkForward.selectedCandidate().evaluation().parameters())) {
                throw new IllegalArgumentException("OOS and walk-forward parameters differ at rank " + rank);
            }
            assessments.add(new FinalCandidateAssessment(
                rank,
                outOfSample.selectedCandidate().evaluation().parameters(),
                outOfSample.selectedCandidate().evaluation().metrics(),
                outOfSample.metrics(),
                walkForward.profitableWindowRate(),
                walkForward.positiveAverageRWindowRate(),
                walkForward.passed()
                    ? CandidateDisposition.PAPER_REVIEW_CANDIDATE
                    : CandidateDisposition.REJECTED,
                walkForward.rejectionReasons()
            ));
        }
        return new ParameterSearchFinalResult(metadata, assessments);
    }

    /** 件数だけが同じ別候補を誤結合しないよう、両結果のrank集合が一意かつ同一であることを確認する。 */
    private void validateCandidateRanks(
        OutOfSampleEvaluationResult outOfSampleResult,
        WalkForwardEvaluationResult walkForwardResult
    ) {
        final Set<Integer> outOfSampleRanks = new HashSet<>();
        outOfSampleResult.evaluations().forEach(candidate ->
            outOfSampleRanks.add(candidate.selectedCandidate().rank())
        );
        final Set<Integer> walkForwardRanks = new HashSet<>();
        walkForwardResult.candidates().forEach(candidate ->
            walkForwardRanks.add(candidate.selectedCandidate().rank())
        );
        if (outOfSampleRanks.size() != outOfSampleResult.evaluations().size()
            || walkForwardRanks.size() != walkForwardResult.candidates().size()
            || !outOfSampleRanks.equals(walkForwardRanks)) {
            throw new IllegalArgumentException("OOS and walk-forward candidate ranks differ or contain duplicates");
        }
    }

    private void validateDatasetIdentity(
        HistoricalDatasetMetadata metadata,
        OutOfSampleEvaluationResult outOfSampleResult,
        WalkForwardEvaluationResult walkForwardResult,
        ParameterSearchSpecification specification
    ) {
        final var expectedStart = specification.periods().outOfSampleFrom().atStartOfDay();
        final var expectedEnd = specification.periods().outOfSampleTo().plusDays(1).atStartOfDay();
        if (!metadata.datasetId().equals(outOfSampleResult.datasetId())
            || !metadata.datasetId().equals(walkForwardResult.datasetId())
            || metadata.currencyPair() != specification.marketData().currencyPair()
            || metadata.timeFrame() != specification.marketData().timeFrame()
            || metadata.priceType() != specification.marketData().priceType()
            || !metadata.requestedFrom().equals(specification.periods().datasetFrom())
            || !metadata.requestedTo().equals(specification.periods().datasetTo())
            || !expectedStart.equals(outOfSampleResult.periodStart())
            || !expectedEnd.equals(outOfSampleResult.periodEndExclusive())
            || !expectedStart.equals(walkForwardResult.periodStart())
            || !expectedEnd.equals(walkForwardResult.periodEndExclusive())) {
            throw new IllegalArgumentException("Final evaluation inputs do not share the configured dataset identity");
        }
    }
}
