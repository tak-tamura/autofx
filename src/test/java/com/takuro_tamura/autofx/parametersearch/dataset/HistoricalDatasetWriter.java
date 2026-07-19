package com.takuro_tamura.autofx.parametersearch.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * 検証済みローソク足を再利用可能なCSVと、再現条件を保持するJSONメタデータとして保存する。
 * 出力先はbuild配下を想定し、ライブ取引やDBの状態は変更しない。
 */
public class HistoricalDatasetWriter {
    private static final String SOURCE = "GMO_COIN_PUBLIC_API";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Clock clock;

    public HistoricalDatasetWriter(Clock clock) {
        this.clock = clock;
    }

    public WrittenDataset write(
        Path outputDirectory,
        List<Candle> candles,
        ParameterSearchSpecification specification,
        DatasetValidationReport validationReport
    ) {
        final String datasetId = datasetId(specification);
        final Path csvPath = outputDirectory.resolve(datasetId + ".csv");
        final Path metadataPath = outputDirectory.resolve(datasetId + ".metadata.json");
        // ハッシュ対象と実際に書き込む内容を同じbyte配列にし、データセット識別のずれを防ぐ。
        final byte[] csv = csv(candles).getBytes(StandardCharsets.UTF_8);
        final HistoricalDatasetMetadata metadata = new HistoricalDatasetMetadata(
            datasetId,
            sha256(csv),
            SOURCE,
            specification.marketData().currencyPair(),
            specification.marketData().timeFrame(),
            specification.marketData().priceType(),
            specification.marketData().timeZone().getId(),
            specification.periods().datasetFrom(),
            specification.periods().datasetTo(),
            validationReport.firstCandleTime(),
            validationReport.lastCandleTime(),
            validationReport.candleCount(),
            validationReport.dataGapCount(),
            0,
            clock.instant()
        );

        try {
            Files.createDirectories(outputDirectory);
            // 生の履歴データを後から上書きすると比較不能になるため、同一IDが存在する場合は失敗させる。
            if (Files.exists(csvPath) || Files.exists(metadataPath)) {
                throw new IllegalStateException("Historical dataset is immutable and already exists: " + datasetId);
            }
            Files.write(csvPath, csv, StandardOpenOption.CREATE_NEW);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write historical dataset: " + datasetId, e);
        }
        return new WrittenDataset(csvPath, metadataPath, metadata);
    }

    private String datasetId(ParameterSearchSpecification specification) {
        // データの市場条件と要求期間がファイル名だけでも判別できる安定したIDを作る。
        return String.format(
            Locale.ROOT,
            "%s_%s_%s_%s_%s",
            specification.marketData().currencyPair().name().toLowerCase(Locale.ROOT),
            specification.marketData().timeFrame().getLabel(),
            specification.periods().datasetFrom().toString().replace("-", ""),
            specification.periods().datasetTo().toString().replace("-", ""),
            specification.marketData().priceType().name().toLowerCase(Locale.ROOT)
        );
    }

    private String csv(List<Candle> candles) {
        final StringBuilder csv = new StringBuilder("time,currencyPair,timeFrame,open,high,low,close\n");
        for (Candle candle : candles) {
            csv.append(candle.getTime()).append(',')
                .append(candle.getCurrencyPair()).append(',')
                .append(candle.getTimeFrame().getLabel()).append(',')
                .append(plain(candle.getOpen().getValue())).append(',')
                .append(plain(candle.getHigh().getValue())).append(',')
                .append(plain(candle.getLow().getValue())).append(',')
                .append(plain(candle.getClose().getValue())).append('\n');
        }
        return csv.toString();
    }

    private String plain(BigDecimal value) {
        return value.toPlainString();
    }

    private String sha256(byte[] content) {
        // 同じIDでも内容が異なる事故を検知できるよう、CSV全体のSHA-256をメタデータへ記録する。
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record WrittenDataset(
        Path csvPath,
        Path metadataPath,
        HistoricalDatasetMetadata metadata
    ) {
    }
}
