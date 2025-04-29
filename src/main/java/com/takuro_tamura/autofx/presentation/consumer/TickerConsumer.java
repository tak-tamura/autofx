package com.takuro_tamura.autofx.presentation.consumer;

import com.takuro_tamura.autofx.application.CandleApplicationService;
import com.takuro_tamura.autofx.application.TradeApplicationService;
import com.takuro_tamura.autofx.application.command.CandleUpsertCommand;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.infrastructure.config.TradeProperties;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PublicApi;
import com.takuro_tamura.autofx.infrastructure.external.response.Ticker;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class TickerConsumer {
    private final Logger log = LoggerFactory.getLogger(TickerConsumer.class);

    private final PublicApi publicApi;

    private final CandleApplicationService candleApplicationService;
    private final TradeApplicationService tradeApplicationService;

    private final List<TimeFrame> timeFrames;
    private final TradeProperties tradeProperties;

    private final Executor executor = Executors.newSingleThreadExecutor();

    public TickerConsumer(
        PublicApi publicApi,
        CandleApplicationService candleApplicationService,
        TradeApplicationService tradeApplicationService,
        TradeProperties tradeProperties
    ) {
        this.publicApi = publicApi;
        this.candleApplicationService = candleApplicationService;
        this.tradeApplicationService = tradeApplicationService;
        this.timeFrames = List.of(
            TimeFrame.MINUTE,
            TimeFrame.MINUTE15,
            TimeFrame.HOUR,
            TimeFrame.HOUR4,
            TimeFrame.DAY
        );
        this.tradeProperties = tradeProperties;
    }

    @PostConstruct
    public void init() {
        executor.execute(this::eventLoop);
    }

    private void eventLoop() {
        log.info("Event loop started");
        while (true) {
            final List<Ticker> tickers = publicApi.getTickers();
            for (Ticker ticker : tickers) {
                for (TimeFrame timeFrame : timeFrames) {
                    final boolean created;
                    try {
                        created = candleApplicationService.upsertCandle(new CandleUpsertCommand(ticker, timeFrame));
                    } catch (Exception e) {
                        log.warn("Failed to upsert candle", e);
                        continue;
                    }

                    if (tradeProperties.getTargetTimeFrame() == timeFrame && created) {
                        try {
                            tradeApplicationService.trade();
                        } catch (Exception e) {
                            log.error("An error occurred while trading", e);
                        }
                    }
                }
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
