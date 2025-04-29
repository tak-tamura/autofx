package com.takuro_tamura.autofx.infrastructure.datasource.entity;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CandleDataModel {
    private Integer id;
    private LocalDateTime time;
    private CurrencyPair currencyPair;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;

    public CandleDataModel() {}

    public CandleDataModel(Candle candle) {
        this.id = candle.getId();
        this.time = candle.getTime();
        this.currencyPair = candle.getCurrencyPair();
        this.open = candle.getOpen().getValue();
        this.close = candle.getClose().getValue();
        this.high = candle.getHigh().getValue();
        this.low = candle.getLow().getValue();
    }

    public Candle toModel(TimeFrame timeFrame) {
        return new Candle(
            this.id,
            this.time,
            this.currencyPair,
            timeFrame,
            new Price(this.open),
            new Price(this.close),
            new Price(this.high),
            new Price(this.low)
        );
    }
}
