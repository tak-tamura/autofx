package com.takuro_tamura.autofx.presentation.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChartResponse {
    private final CurrencyPair currencyPair;
    private final List<CandleDto> candles;
    private final Indicator indicator;
    private final List<OrderRecord> orders;
    private final Double profit;

    @Builder
    public ChartResponse(
        CurrencyPair currencyPair,
        List<CandleDto> candles,
        List<MaRecord> smas,
        List<MaRecord> emas,
        BBandsRecord bbands,
        IchimokuRecord ichimoku,
        RsiRecord rsi,
        MacdRecord macd,
        List<Double> adx,
        List<OrderRecord> orders,
        Double profit
    ) {
        this.currencyPair = currencyPair;
        this.candles = candles;
        this.orders = orders;
        this.profit = profit;
        this.indicator = Indicator.builder()
            .smas(smas)
            .emas(emas)
            .bbands(bbands)
            .ichimoku(ichimoku)
            .rsi(rsi)
            .macd(macd)
            .adx(adx)
            .build();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Indicator(
        List<MaRecord> smas,
        List<MaRecord> emas,
        BBandsRecord bbands,
        IchimokuRecord ichimoku,
        RsiRecord rsi,
        MacdRecord macd,
        List<Double> adx
    ) {
        @Builder
        public Indicator {
        }
    }
}
