package com.takuro_tamura.autofx.parametersearch.paper;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * preparePaperCandidate Gradleタスクの入口。
 * 明示されたローカルファイルだけを読み、外部API、DB、注文処理には接続しない。
 */
@Tag("paper-candidate-preparation")
class PreparePaperCandidateTaskTest {
    @Test
    void preparesExplicitlySelectedCandidateForManualReview() {
        final Path manifestPath = requiredPath("paperManifest");
        final Path currentConfigurationPath = requiredPath("paperCurrentConfiguration");
        final int candidateRank = requiredPositiveInt("paperCandidateRank");
        final Path outputDirectory = Path.of(
            System.getProperty("paperPreparationOutput", "build/reports/parameter-search/paper-preparation")
        );

        final var reader = new PaperCandidatePreparationReader();
        final var plan = new PaperCandidatePreparer(Clock.systemUTC()).prepare(
            reader.readManifest(manifestPath),
            candidateRank,
            reader.readCurrentConfiguration(currentConfigurationPath)
        );
        final Path output = new PaperCandidatePreparationWriter().write(outputDirectory, plan);

        assertThat(output).exists();
        assertThat(plan.manualReviewRequired()).isTrue();
        assertThat(plan.applyAllowed()).isFalse();
        assertThat(plan.liveTradingAllowed()).isFalse();
    }

    private Path requiredPath(String property) {
        final String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required system property is missing: " + property);
        }
        return Path.of(value);
    }

    private int requiredPositiveInt(String property) {
        final String value = System.getProperty(property);
        try {
            final int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // 共通の入力エラーへまとめ、Gradleプロパティ名を明示する。
        }
        throw new IllegalArgumentException("Positive integer system property is required: " + property);
    }
}
