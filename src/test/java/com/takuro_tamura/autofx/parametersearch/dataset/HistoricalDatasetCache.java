package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 指定条件の履歴ファイルが揃っていればキャッシュから読み込み、なければGMO公開APIから取得して保存する。
 * キャッシュ異常時にAPI結果で上書きすると再現性を失うため、不完全・不正なキャッシュは例外として扱う。
 */
public class HistoricalDatasetCache {
    private final HistoricalDatasetFetcher fetcher;
    private final HistoricalDatasetValidator validator;
    private final HistoricalDatasetWriter writer;
    private final HistoricalDatasetReader reader;

    public HistoricalDatasetCache(
        HistoricalDatasetFetcher fetcher,
        HistoricalDatasetValidator validator,
        HistoricalDatasetWriter writer,
        HistoricalDatasetReader reader
    ) {
        this.fetcher = fetcher;
        this.validator = validator;
        this.writer = writer;
        this.reader = reader;
    }

    /**
     * 同じ市場条件・期間のCSVとmetadataが存在する場合はAPIを呼ばずに返す。
     * 初回取得時も保存前と同じ品質検証を行い、戻り値でキャッシュ利用の有無を確認できる。
     */
    public LoadedDataset loadOrFetch(
        Path cacheDirectory,
        ParameterSearchSpecification specification
    ) {
        if (cacheDirectory == null || specification == null) {
            throw new IllegalArgumentException("Cache directory and specification are required");
        }
        final HistoricalDatasetFiles files = HistoricalDatasetFiles.from(cacheDirectory, specification);
        final boolean csvExists = Files.exists(files.csvPath());
        final boolean metadataExists = Files.exists(files.metadataPath());

        if (csvExists != metadataExists) {
            // 片方だけでは内容と取得条件を相互検証できないため、APIへフォールバックしない。
            throw new IllegalStateException("Historical dataset cache is incomplete: " + files.datasetId());
        }
        if (csvExists) {
            final HistoricalDatasetReader.ReadDataset cached = reader.read(files, specification);
            final DatasetValidationReport report = validator.validate(cached.candles(), specification);
            validateReportMatchesMetadata(report, cached.metadata());
            return new LoadedDataset(cached.candles(), cached.metadata(), true);
        }

        final List<Candle> fetched = fetcher.fetch(specification);
        final DatasetValidationReport report = validator.validate(fetched, specification);
        final HistoricalDatasetWriter.WrittenDataset written = writer.write(
            cacheDirectory, fetched, specification, report
        );
        return new LoadedDataset(fetched, written.metadata(), false);
    }

    private void validateReportMatchesMetadata(
        DatasetValidationReport report,
        HistoricalDatasetMetadata metadata
    ) {
        if (report.candleCount() != metadata.candleCount()
            || report.dataGapCount() != metadata.dataGapCount()
            || !report.firstCandleTime().equals(metadata.firstCandleTime())
            || !report.lastCandleTime().equals(metadata.lastCandleTime())
            || metadata.duplicateCount() != 0) {
            throw new IllegalStateException("Historical dataset cache validation differs from metadata");
        }
    }

    public record LoadedDataset(
        List<Candle> candles,
        HistoricalDatasetMetadata metadata,
        boolean cacheHit
    ) {
        public LoadedDataset {
            candles = List.copyOf(candles);
        }
    }
}
