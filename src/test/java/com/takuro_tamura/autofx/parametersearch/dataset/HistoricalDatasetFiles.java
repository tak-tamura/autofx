package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;

import java.nio.file.Path;
import java.util.Locale;

/** 同じ探索条件から、キャッシュのIDとCSV・metadataのパスを一意に組み立てる。 */
public record HistoricalDatasetFiles(
    String datasetId,
    Path csvPath,
    Path metadataPath
) {
    public static HistoricalDatasetFiles from(
        Path directory,
        ParameterSearchSpecification specification
    ) {
        if (directory == null || specification == null) {
            throw new IllegalArgumentException("Dataset directory and specification are required");
        }
        final String datasetId = String.format(
            Locale.ROOT,
            "%s_%s_%s_%s_%s",
            specification.marketData().currencyPair().name().toLowerCase(Locale.ROOT),
            specification.marketData().timeFrame().getLabel(),
            specification.periods().datasetFrom().toString().replace("-", ""),
            specification.periods().datasetTo().toString().replace("-", ""),
            specification.marketData().priceType().name().toLowerCase(Locale.ROOT)
        );
        return new HistoricalDatasetFiles(
            datasetId,
            directory.resolve(datasetId + ".csv"),
            directory.resolve(datasetId + ".metadata.json")
        );
    }
}
