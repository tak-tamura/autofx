package com.takuro_tamura.autofx.parametersearch.paper;

import com.takuro_tamura.autofx.domain.strategy.EmaCrossStrategy;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterSet;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;

/**
 * manifestをfail closedで検証し、明示された順位の候補だけをレビュー計画へ変換する。
 * DB、取引状態、注文ポートには依存せず、副作用は持たない。
 */
public class PaperCandidatePreparer {
    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private final Clock clock;

    public PaperCandidatePreparer(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required");
        }
        this.clock = clock;
    }

    public PaperCandidatePreparationPlan prepare(
        PaperCandidateManifest manifest,
        int candidateRank,
        PaperCurrentConfiguration current
    ) {
        validate(manifest, current);
        if (candidateRank <= 0) {
            throw new IllegalArgumentException("Candidate rank must be explicitly specified");
        }
        final var candidate = manifest.candidates().stream()
            .filter(value -> value.inSampleRank() == candidateRank)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Candidate rank is not present in manifest: " + candidateRank));
        final StrategyParameterSet proposed = candidate.parameters();
        return new PaperCandidatePreparationPlan(
            1,
            clock.instant(),
            manifest.dataset().datasetId(),
            manifest.dataset().sha256(),
            candidateRank,
            true,
            false,
            false,
            current,
            proposed,
            manifest.riskParameters().atrPeriod(),
            manifest.executionAssumptions(),
            manifest.riskParameters(),
            differences(current, proposed, manifest.riskParameters().atrPeriod())
        );
    }

    private void validate(PaperCandidateManifest manifest, PaperCurrentConfiguration current) {
        if (manifest == null || current == null) {
            throw new IllegalArgumentException("Manifest and current configuration are required");
        }
        if (manifest.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported paper candidate manifest schema: " + manifest.schemaVersion());
        }
        if (!manifest.manualReviewRequired() || manifest.liveTradingAllowed()) {
            throw new IllegalArgumentException("Manifest safety flags do not permit paper preparation");
        }
        if (!EmaCrossStrategy.class.getName().equals(manifest.strategyClass())) {
            throw new IllegalArgumentException("Unsupported strategy class: " + manifest.strategyClass());
        }
        if (manifest.dataset() == null || manifest.executionAssumptions() == null
            || manifest.riskParameters() == null || manifest.walkForwardCriteria() == null
            || manifest.candidates() == null || manifest.candidates().isEmpty()) {
            throw new IllegalArgumentException("Manifest is incomplete");
        }
        if (current.currencyPair() != manifest.dataset().currencyPair()
            || current.timeFrame() != manifest.dataset().timeFrame()) {
            throw new IllegalArgumentException("Current market identity differs from manifest dataset");
        }
        final var ranks = new HashSet<Integer>();
        for (PaperCandidateManifest.PaperCandidate candidate : manifest.candidates()) {
            if (candidate == null || candidate.inSampleRank() <= 0 || candidate.parameters() == null
                || candidate.inSampleMetrics() == null || candidate.outOfSampleMetrics() == null
                || candidate.profitableWindowRate() == null || candidate.positiveAverageRWindowRate() == null
                || !ranks.add(candidate.inSampleRank())) {
                throw new IllegalArgumentException("Manifest contains an invalid or duplicate candidate");
            }
        }
    }

    private List<PaperCandidatePreparationPlan.ParameterDifference> differences(
        PaperCurrentConfiguration current,
        StrategyParameterSet proposed,
        int proposedAtrPeriod
    ) {
        final StrategyParameterSet existing = current.strategyParameters();
        return List.of(
            difference("emaShortPeriod", existing.emaShortPeriod(), proposed.emaShortPeriod()),
            difference("emaLongPeriod", existing.emaLongPeriod(), proposed.emaLongPeriod()),
            difference("rsiPeriod", existing.rsiPeriod(), proposed.rsiPeriod()),
            difference("macdFastPeriod", existing.macdFastPeriod(), proposed.macdFastPeriod()),
            difference("macdSlowPeriod", existing.macdSlowPeriod(), proposed.macdSlowPeriod()),
            difference("macdSignalPeriod", existing.macdSignalPeriod(), proposed.macdSignalPeriod()),
            difference("bBandsPeriod", existing.bBandsPeriod(), proposed.bBandsPeriod()),
            difference("bBandsMultiplier", existing.bBandsMultiplier(), proposed.bBandsMultiplier()),
            difference("adxPeriod", existing.adxPeriod(), proposed.adxPeriod()),
            difference("adxThreshold", existing.adxThreshold(), proposed.adxThreshold()),
            difference("atrPeriod", current.atrPeriod(), proposedAtrPeriod)
        );
    }

    private PaperCandidatePreparationPlan.ParameterDifference difference(String name, Object current, Object proposed) {
        final String currentValue = displayValue(current);
        final String proposedValue = displayValue(proposed);
        return new PaperCandidatePreparationPlan.ParameterDifference(
            name, currentValue, proposedValue, !currentValue.equals(proposedValue)
        );
    }

    /** 差分欄は文字列なので、BigDecimalだけは指数表記を避けて人が読みやすい値にする。 */
    private String displayValue(Object value) {
        return value instanceof BigDecimal decimal ? decimal.toPlainString() : value.toString();
    }
}
