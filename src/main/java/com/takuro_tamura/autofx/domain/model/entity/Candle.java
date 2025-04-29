package com.takuro_tamura.autofx.domain.model.entity;

import com.takuro_tamura.autofx.application.command.CandleUpsertCommand;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class Candle {
    @Setter
    private Integer id;
    private final LocalDateTime time;
    private final CurrencyPair currencyPair;
    private final TimeFrame timeFrame;
    private final Price open;
    @Setter
    private Price close;
    @Setter
    private Price high;
    @Setter
    private Price low;

    public Candle(CandleUpsertCommand command) {
        this.id = null;
        this.time = command.getTime();
        this.currencyPair = command.getCurrencyPair();
        this.timeFrame = command.getTimeFrame();
        this.open = command.getPrice();
        this.close = command.getPrice();
        this.high = command.getPrice();
        this.low = command.getPrice();
    }

    @Builder
    public Candle(
        Integer id,
        LocalDateTime time,
        CurrencyPair currencyPair,
        TimeFrame timeFrame,
        Price open,
        Price close,
        Price high,
        Price low
    ) {
        this.id = id;
        this.time = time;
        this.currencyPair = currencyPair;
        this.timeFrame = timeFrame;
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
    }

    public boolean shouldUpdateHighPrice(Price currentPrice) {
        return this.high.compareTo(currentPrice) < 0;
    }

    public boolean shouldUpdateLowPrice(Price currentPrice) {
        return this.high.compareTo(currentPrice) > 0;
    }
}
