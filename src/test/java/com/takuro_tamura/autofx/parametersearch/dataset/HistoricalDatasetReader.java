package com.takuro_tamura.autofx.parametersearch.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 保存済みCSVとmetadataを読み込み、探索条件と内容の整合性を確認してローソク足へ復元する。
 * 改変・別期間・別市場のファイルを誤ってバックテストへ渡さないことを責務とする。
 */
public class HistoricalDatasetReader {
    private static final String CSV_HEADER = "time,currencyPair,timeFrame,open,high,low,close";
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public ReadDataset read(
        HistoricalDatasetFiles files,
        ParameterSearchSpecification specification
    ) {
        if (files == null || specification == null) {
            throw new IllegalArgumentException("Dataset files and specification are required");
        }
        try {
            final byte[] csv = Files.readAllBytes(files.csvPath());
            final HistoricalDatasetMetadata metadata = objectMapper.readValue(
                files.metadataPath().toFile(), HistoricalDatasetMetadata.class
            );
            validateMetadata(files, specification, metadata, csv);
            final List<Candle> candles = parseCsv(new String(csv, StandardCharsets.UTF_8));
            validateParsedCandles(metadata, candles);
            return new ReadDataset(candles, metadata);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read historical dataset cache: " + files.datasetId(), e);
        }
    }

    private void validateMetadata(
        HistoricalDatasetFiles files,
        ParameterSearchSpecification specification,
        HistoricalDatasetMetadata metadata,
        byte[] csv
    ) {
        if (!files.datasetId().equals(metadata.datasetId())
            || specification.marketData().currencyPair() != metadata.currencyPair()
            || specification.marketData().timeFrame() != metadata.timeFrame()
            || specification.marketData().priceType() != metadata.priceType()
            || !specification.marketData().timeZone().getId().equals(metadata.timeZone())
            || !specification.periods().datasetFrom().equals(metadata.requestedFrom())
            || !specification.periods().datasetTo().equals(metadata.requestedTo())) {
            throw new IllegalStateException("Historical dataset cache does not match requested conditions");
        }
        if (!HistoricalDatasetHash.sha256(csv).equals(metadata.sha256())) {
            throw new IllegalStateException("Historical dataset cache checksum does not match metadata");
        }
    }

    private List<Candle> parseCsv(String csv) {
        final String[] lines = csv.split("\\R");
        if (lines.length == 0 || !CSV_HEADER.equals(lines[0])) {
            throw new IllegalStateException("Historical dataset cache has an invalid CSV header");
        }
        final List<Candle> candles = new ArrayList<>(Math.max(0, lines.length - 1));
        for (int lineNumber = 1; lineNumber < lines.length; lineNumber++) {
            if (lines[lineNumber].isBlank()) {
                continue;
            }
            final String[] columns = lines[lineNumber].split(",", -1);
            if (columns.length != 7) {
                throw new IllegalStateException("Historical dataset cache has an invalid row: " + (lineNumber + 1));
            }
            try {
                candles.add(Candle.builder()
                    .time(LocalDateTime.parse(columns[0]))
                    .currencyPair(CurrencyPair.valueOf(columns[1]))
                    .timeFrame(TimeFrame.fromLabel(columns[2]))
                    .open(new Price(columns[3]))
                    .high(new Price(columns[4]))
                    .low(new Price(columns[5]))
                    .close(new Price(columns[6]))
                    .build());
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                    "Historical dataset cache contains an invalid value at row " + (lineNumber + 1), e
                );
            }
        }
        return List.copyOf(candles);
    }

    private void validateParsedCandles(HistoricalDatasetMetadata metadata, List<Candle> candles) {
        if (candles.isEmpty()
            || candles.size() != metadata.candleCount()
            || !candles.get(0).getTime().equals(metadata.firstCandleTime())
            || !candles.get(candles.size() - 1).getTime().equals(metadata.lastCandleTime())) {
            throw new IllegalStateException("Historical dataset cache content does not match metadata");
        }
    }

    public record ReadDataset(List<Candle> candles, HistoricalDatasetMetadata metadata) {
        public ReadDataset {
            candles = List.copyOf(candles);
        }
    }
}
