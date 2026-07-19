package com.takuro_tamura.autofx.parametersearch.dataset;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HistoricalDatasetValidatorTest {
    private final HistoricalDatasetValidator validator = new HistoricalDatasetValidator();

    @Test
    void validatesOrderedDatasetAndRecordsTimeGaps() {
        final var specification = DatasetTestFixtures.specification();
        final var candles = List.of(
            DatasetTestFixtures.candle(LocalDateTime.of(2025, 1, 1, 6, 0), "150", "151", "149", "150.5"),
            DatasetTestFixtures.candle(LocalDateTime.of(2025, 1, 1, 7, 0), "150.5", "151", "150", "150.8"),
            DatasetTestFixtures.candle(LocalDateTime.of(2025, 1, 1, 9, 0), "150.8", "151", "150", "150.9")
        );

        final DatasetValidationReport report = validator.validate(candles, specification);

        assertThat(report.candleCount()).isEqualTo(3);
        assertThat(report.dataGapCount()).isEqualTo(1);
        assertThat(report.firstCandleTime()).isEqualTo(candles.get(0).getTime());
        assertThat(report.lastCandleTime()).isEqualTo(candles.get(2).getTime());
    }

    @Test
    void rejectsDuplicateTimestamps() {
        final var specification = DatasetTestFixtures.specification();
        final var candle = DatasetTestFixtures.candle(
            LocalDateTime.of(2025, 1, 1, 6, 0), "150", "151", "149", "150.5"
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> validator.validate(List.of(candle, candle), specification))
            .withMessageContaining("Duplicate");
    }

    @Test
    void rejectsInvalidOhlcRelationship() {
        final var specification = DatasetTestFixtures.specification();
        final var candle = DatasetTestFixtures.candle(
            LocalDateTime.of(2025, 1, 1, 6, 0), "150", "149", "148", "150.5"
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> validator.validate(List.of(candle), specification))
            .withMessageContaining("Invalid OHLC");
    }

    @Test
    void rejectsTimestampNotAlignedToConfiguredTimeframe() {
        final var specification = DatasetTestFixtures.specification();
        final var candle = DatasetTestFixtures.candle(
            LocalDateTime.of(2025, 1, 1, 6, 30), "150", "151", "149", "150.5"
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> validator.validate(List.of(candle), specification))
            .withMessageContaining("not aligned");
    }

    @Test
    void acceptsNextCalendarDayCandleBeforeGmoApiDateRollover() {
        final var specification = DatasetTestFixtures.specification();
        final var candle = DatasetTestFixtures.candle(
            LocalDateTime.of(2025, 1, 3, 0, 0), "150", "151", "149", "150.5"
        );

        final DatasetValidationReport report = validator.validate(List.of(candle), specification);

        assertThat(report.candleCount()).isEqualTo(1);
    }

    @Test
    void rejectsCandleAfterLastRequestedGmoApiDateRollover() {
        final var specification = DatasetTestFixtures.specification();
        final var candle = DatasetTestFixtures.candle(
            LocalDateTime.of(2025, 1, 3, 6, 0), "150", "151", "149", "150.5"
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> validator.validate(List.of(candle), specification))
            .withMessageContaining("outside requested dataset period");
    }
}
