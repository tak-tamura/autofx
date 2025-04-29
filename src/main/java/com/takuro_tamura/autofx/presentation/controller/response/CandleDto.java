package com.takuro_tamura.autofx.presentation.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class CandleDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "Asia/Tokyo")
    private final LocalDateTime time;
    private final CurrencyPair currencyPair;
    private final TimeFrame timeFrame;
    private final BigDecimal open;
    private final BigDecimal close;
    private final BigDecimal high;
    private final BigDecimal low;

    public CandleDto(Candle candle) {
        this.time = candle.getTime();
        this.currencyPair = candle.getCurrencyPair();
        this.timeFrame = candle.getTimeFrame();
        this.open = candle.getOpen().getValue();
        this.close = candle.getClose().getValue();
        this.high = candle.getHigh().getValue();
        this.low = candle.getLow().getValue();
    }
}
