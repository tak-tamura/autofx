package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.infrastructure.external.adapter.PublicApi;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecificationLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GMOコイン公開APIから固定データセットを生成する手動実行専用テスト。
 * Spring Bootを起動しないため、プライベートAPI、WebSocket、注文処理には接続しない。
 */
@Tag("external-market-data")
@Disabled("Run manually only when refreshing the historical dataset")
class GmoCoinHistoricalDatasetFetchTest {
    private static final String KLINES_URL = "https://forex-api.coin.z.com/public/v1/klines";

    @Test
    void fetchesValidatesAndWritesConfiguredDataset() {
        // SpringのApplicationContextを起動せず、公開KLine APIに必要な部品だけを手動生成する。
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final PublicApi publicApi = new PublicApi("unused", KLINES_URL, RestClient.create());
        final HistoricalDatasetFetcher fetcher = new HistoricalDatasetFetcher(
            publicApi,
            new KlineCandleMapper(),
            Clock.systemUTC(),
            () -> LockSupport.parkNanos(Duration.ofMillis(250).toNanos())
        );

        // 取得、品質検証、固定ファイル保存を分離し、異常データは保存前に停止させる。
        final var candles = fetcher.fetch(specification);
        final DatasetValidationReport report = new HistoricalDatasetValidator()
            .validate(candles, specification);
        final var written = new HistoricalDatasetWriter(Clock.systemUTC()).write(
            Path.of("build", "reports", "parameter-search", "datasets"),
            candles,
            specification,
            report
        );

        assertThat(written.csvPath()).exists();
        assertThat(written.metadataPath()).exists();
    }
}
