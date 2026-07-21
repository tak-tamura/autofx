package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoricalDatasetCacheTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void fetchesOnceAndUsesValidatedFilesForTheSameConditions() {
        final var specification = DatasetTestFixtures.specification();
        final List<Candle> fetchedCandles = candles();
        final HistoricalDatasetFetcher fetcher = mock(HistoricalDatasetFetcher.class);
        when(fetcher.fetch(specification)).thenReturn(fetchedCandles);
        final HistoricalDatasetCache cache = cache(fetcher);

        final var first = cache.loadOrFetch(temporaryDirectory, specification);
        final var second = cache.loadOrFetch(temporaryDirectory, specification);

        assertThat(first.cacheHit()).isFalse();
        assertThat(second.cacheHit()).isTrue();
        assertThat(second.metadata()).isEqualTo(first.metadata());
        assertThat(second.candles()).extracting(Candle::getTime)
            .containsExactly(
                LocalDateTime.of(2025, 1, 1, 6, 0),
                LocalDateTime.of(2025, 1, 1, 7, 0)
            );
        verify(fetcher, times(1)).fetch(specification);
    }

    @Test
    void rejectsModifiedCacheWithoutFetchingReplacementData() throws Exception {
        final var specification = DatasetTestFixtures.specification();
        final HistoricalDatasetFetcher fetcher = mock(HistoricalDatasetFetcher.class);
        when(fetcher.fetch(specification)).thenReturn(candles());
        final HistoricalDatasetCache cache = cache(fetcher);
        cache.loadOrFetch(temporaryDirectory, specification);
        final HistoricalDatasetFiles files = HistoricalDatasetFiles.from(temporaryDirectory, specification);
        Files.writeString(files.csvPath(), "tampered", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);

        assertThatIllegalStateException()
            .isThrownBy(() -> cache.loadOrFetch(temporaryDirectory, specification))
            .withMessageContaining("checksum");
        verify(fetcher, times(1)).fetch(specification);
    }

    @Test
    void rejectsCacheWhenOnlyOneRequiredFileExists() throws Exception {
        final var specification = DatasetTestFixtures.specification();
        final HistoricalDatasetFetcher fetcher = mock(HistoricalDatasetFetcher.class);
        final HistoricalDatasetFiles files = HistoricalDatasetFiles.from(temporaryDirectory, specification);
        Files.writeString(files.csvPath(), "incomplete", StandardCharsets.UTF_8);

        assertThatIllegalStateException()
            .isThrownBy(() -> cache(fetcher).loadOrFetch(temporaryDirectory, specification))
            .withMessageContaining("incomplete");
        verify(fetcher, times(0)).fetch(specification);
    }

    private HistoricalDatasetCache cache(HistoricalDatasetFetcher fetcher) {
        final Clock clock = Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC);
        return new HistoricalDatasetCache(
            fetcher,
            new HistoricalDatasetValidator(),
            new HistoricalDatasetWriter(clock),
            new HistoricalDatasetReader()
        );
    }

    private List<Candle> candles() {
        return List.of(
            DatasetTestFixtures.candle(
                LocalDateTime.of(2025, 1, 1, 6, 0), "150.0", "151.0", "149.0", "150.5"
            ),
            DatasetTestFixtures.candle(
                LocalDateTime.of(2025, 1, 1, 7, 0), "150.5", "151.2", "150.0", "151.0"
            )
        );
    }
}
