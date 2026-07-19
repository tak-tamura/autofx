package com.takuro_tamura.autofx.parametersearch.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class HistoricalDatasetWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesImmutableCsvAndReproducibilityMetadata() throws Exception {
        final var specification = DatasetTestFixtures.specification();
        final var candles = List.of(
            DatasetTestFixtures.candle(LocalDateTime.of(2025, 1, 1, 6, 0), "150.0", "151.0", "149.0", "150.5"),
            DatasetTestFixtures.candle(LocalDateTime.of(2025, 1, 1, 7, 0), "150.5", "151.2", "150.0", "151.0")
        );
        final DatasetValidationReport report = new HistoricalDatasetValidator()
            .validate(candles, specification);
        final Clock clock = Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC);

        final var written = new HistoricalDatasetWriter(clock)
            .write(temporaryDirectory, candles, specification, report);

        assertThat(written.csvPath()).exists();
        assertThat(written.metadataPath()).exists();
        assertThat(Files.readString(written.csvPath(), StandardCharsets.UTF_8))
            .contains("time,currencyPair,timeFrame,open,high,low,close")
            .contains("2025-01-01T06:00,USD_JPY,1h,150.0,151.0,149.0,150.5");
        assertThat(written.metadata().sha256()).hasSize(64);
        assertThat(written.metadata().candleCount()).isEqualTo(2);
        assertThat(written.metadata().duplicateCount()).isZero();

        final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        final HistoricalDatasetMetadata restored = objectMapper.readValue(
            written.metadataPath().toFile(),
            HistoricalDatasetMetadata.class
        );
        assertThat(restored).isEqualTo(written.metadata());

        assertThatIllegalStateException()
            .isThrownBy(() -> new HistoricalDatasetWriter(clock)
                .write(temporaryDirectory, candles, specification, report))
            .withMessageContaining("immutable");
    }
}
