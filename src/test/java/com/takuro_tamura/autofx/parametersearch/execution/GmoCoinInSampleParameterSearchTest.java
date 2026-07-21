package com.takuro_tamura.autofx.parametersearch.execution;

import com.takuro_tamura.autofx.domain.backtest.BacktestExecutionPolicy;
import com.takuro_tamura.autofx.domain.backtest.BacktestMetricsCalculator;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.service.BackTestService;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.port.OrderCachePort;
import com.takuro_tamura.autofx.domain.service.port.OrderPlacementPort;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PublicApi;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecificationLoader;
import com.takuro_tamura.autofx.parametersearch.config.StrategyParameterCandidateGenerator;
import com.takuro_tamura.autofx.parametersearch.dataset.HistoricalDatasetFetcher;
import com.takuro_tamura.autofx.parametersearch.dataset.HistoricalDatasetValidator;
import com.takuro_tamura.autofx.parametersearch.dataset.KlineCandleMapper;
import com.takuro_tamura.autofx.parametersearch.selection.InSampleCandidateRanker;
import com.takuro_tamura.autofx.parametersearch.selection.InSampleSelectionWriter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.nio.file.Path;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * GMOコイン公開APIの検証済みローソク足で、In-sample探索全体を確認する手動テスト。
 * Springを起動せず、注文ポートもmockに固定するため実注文は送信されない。
 */
@Tag("external-market-data")
@Disabled("Run manually only when evaluating the configured parameter candidates")
class GmoCoinInSampleParameterSearchTest {
    private static final String KLINES_URL = "https://forex-api.coin.z.com/public/v1/klines";

    private Logger log = LoggerFactory.getLogger(GmoCoinInSampleParameterSearchTest.class);

    @Test
    void evaluatesConfiguredCandidatesWithPublicHistoricalData() {
        final var specification = ParameterSearchSpecificationLoader.load("parameter-search.properties");
        final PublicApi publicApi = new PublicApi("unused", KLINES_URL, RestClient.create());
        final HistoricalDatasetFetcher fetcher = new HistoricalDatasetFetcher(
            publicApi,
            new KlineCandleMapper(),
            Clock.systemUTC(),
            () -> LockSupport.parkNanos(Duration.ofMillis(250).toNanos())
        );

        // Phase 2と同じ品質検証を通過したデータだけを探索処理へ渡す。
        final var candles = fetcher.fetch(specification);
        final var validation = new HistoricalDatasetValidator().validate(candles, specification);

        final TradeConfigParameterService databaseConfig = mock(TradeConfigParameterService.class);
        final CandleService candleService = new CandleService(databaseConfig, mock(CandleRepository.class));
        final OrderService orderService = new OrderService(
            mock(OrderPlacementPort.class),
            mock(OrderCachePort.class),
            candleService,
            databaseConfig
        );
        final BackTestService backTestService = new BackTestService(
            candleService,
            orderService,
            mock(CandleRepository.class),
            databaseConfig,
            new BacktestExecutionPolicy()
        );
        final InSampleParameterSearchRunner runner = new InSampleParameterSearchRunner(
            candleService,
            backTestService,
            new BacktestMetricsCalculator(),
            new StrategyParameterCandidateGenerator()
        );

        final InSampleParameterSearchResult result = runner.run(
            "gmo-public-api-" + specification.periods().datasetFrom() + "-" + specification.periods().datasetTo(),
            candles,
            specification
        );

        // Out-of-sampleデータを参照せず候補を固定し、順位表と取引台帳をレビュー用に保存する。
        final var selection = new InSampleCandidateRanker().rank(result, specification.selectionCriteria());
        final var written = new InSampleSelectionWriter().write(
            Path.of("build", "reports", "parameter-search", "selections"),
            selection,
            specification
        );

        assertThat(validation.candleCount()).isEqualTo(candles.size());
        assertThat(result.evaluations()).hasSize(specification.strategySearchSpace().candidateCount());
        assertThat(result.evaluations()).allSatisfy(evaluation ->
            assertThat(evaluation.backtestResult().assumptions()).isEqualTo(specification.executionAssumptions())
        );
        assertThat(written.summaryPath()).exists();
        assertThat(written.tradesPath()).exists();
    }
}
