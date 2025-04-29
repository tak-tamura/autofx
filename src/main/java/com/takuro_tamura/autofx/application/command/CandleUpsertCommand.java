package com.takuro_tamura.autofx.application.command;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.infrastructure.external.response.FxStatus;
import com.takuro_tamura.autofx.infrastructure.external.response.Ticker;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@ToString
public class CandleUpsertCommand {
    private final LocalDateTime time;
    private final CurrencyPair currencyPair;
    private final TimeFrame timeFrame;
    private final Price price;
    private final FxStatus status;

    public CandleUpsertCommand(Ticker ticker, TimeFrame timeFrame) {
        final var time = LocalDateTime.ofInstant(ticker.getTimestamp(), ZoneId.of("Asia/Tokyo"));
        this.time = timeFrame.truncateTime(time);
        this.currencyPair = ticker.getSymbol();
        this.timeFrame = timeFrame;
        this.price = Price.getMidPrice(ticker.getBid(), ticker.getAsk());
        this.status = ticker.getStatus();
    }
}
