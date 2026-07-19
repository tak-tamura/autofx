package com.takuro_tamura.autofx.parametersearch.dataset;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PublicApi;
import com.takuro_tamura.autofx.infrastructure.external.response.Kline;
import com.takuro_tamura.autofx.parametersearch.config.ParameterSearchSpecification;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 設定期間をGMOコインのAPI日付単位で走査し、バックテスト用ローソク足をメモリ上に構築する。
 * DBへ保存せず、公開API以外の接続や注文処理も行わない。
 */
public class HistoricalDatasetFetcher {
    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PublicApi publicApi;
    private final KlineCandleMapper mapper;
    private final Clock clock;
    private final RequestPacer requestPacer;

    public HistoricalDatasetFetcher(
        PublicApi publicApi,
        KlineCandleMapper mapper,
        Clock clock,
        RequestPacer requestPacer
    ) {
        this.publicApi = publicApi;
        this.mapper = mapper;
        this.clock = clock;
        this.requestPacer = requestPacer;
    }

    public List<Candle> fetch(ParameterSearchSpecification specification) {
        // PublicApi.getKlines()は現状priceType=ASK固定なので、誤ったBIDデータとして扱うことを防ぐ。
        if (specification.marketData().priceType()
            != com.takuro_tamura.autofx.parametersearch.config.MarketPriceType.ASK) {
            throw new IllegalArgumentException("PublicApi.getKlines currently supports ASK datasets only");
        }

        final List<Candle> candles = new ArrayList<>();
        final var periods = specification.periods();
        // APIの日付はJST 6:00区切りだが、リクエスト自体は設定されたAPI日付を1日ずつ渡す。
        for (LocalDate date = periods.datasetFrom(); !date.isAfter(periods.datasetTo()); date = date.plusDays(1)) {
            final List<Kline> klines = publicApi.getKlines(
                specification.marketData().currencyPair(),
                specification.marketData().timeFrame(),
                date.format(API_DATE_FORMAT)
            );
            if (klines == null) {
                throw new IllegalStateException("Kline API returned null data for " + date);
            }
            for (Kline kline : klines) {
                final Candle candle = mapper.map(kline, specification.marketData());
                // 現在時刻はClockから取得し、テストで形成中足の境界を固定できるようにする。
                if (!specification.marketData().excludeIncompleteCandle()
                    || isCompleted(candle, clock, specification.marketData().timeZone())) {
                    candles.add(candle);
                }
            }
            // 長期間取得時に公開APIへ短時間で大量アクセスしないよう、呼び出し側が待機方法を注入する。
            requestPacer.afterRequest();
        }
        // 取得後に呼び出し元が要素を追加・削除してデータセットを変えないよう固定する。
        return List.copyOf(candles);
    }

    private boolean isCompleted(Candle candle, Clock clock, java.time.ZoneId timeZone) {
        final LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), timeZone);
        return !nextOpenTime(candle.getTime(), candle.getTimeFrame()).isAfter(now);
    }

    static LocalDateTime nextOpenTime(LocalDateTime time, TimeFrame timeFrame) {
        // 現在の足の終了時刻は、同時に次の足が始まる時刻として求める。
        return switch (timeFrame) {
            case MINUTE -> time.plusMinutes(1);
            case MINUTE15 -> time.plusMinutes(15);
            case HOUR -> time.plusHours(1);
            case HOUR4 -> time.plusHours(4);
            case DAY -> time.plusDays(1);
            case WEEK -> time.plusWeeks(1);
            case MONTH -> time.plusMonths(1);
        };
    }

    @FunctionalInterface
    public interface RequestPacer {
        void afterRequest();

        static RequestPacer noDelay() {
            return () -> { };
        }
    }
}
