package com.takuro_tamura.autofx.application.command.chart;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import lombok.Data;

@Data
public class GetChartCommand {
    private CurrencyPair currencyPair;
    private Integer limit;
    private TimeFrame timeFrame;
    private MaParam sma;
    private MaParam ema;
    private BBandsParam bbands;
    private IchimokuParam ichimoku;
    private RsiParam rsi;
    private MacdParam macd;
    private boolean includeOrder;

    public boolean isSmaEnabled() {
        return sma != null && sma.isEnable();
    }

    public boolean isEmaEnabled() {
        return ema != null && ema.isEnable();
    }

    public boolean isBBandsEnabled() {
        return bbands != null && bbands.isEnable();
    }

    public boolean isIchimokuEnabled() {
        return ichimoku != null && ichimoku.isEnable();
    }

    public boolean isRsiEnabled() {
        return rsi != null && rsi.isEnable();
    }

    public boolean isMacdEnabled() {
        return macd != null && macd.isEnable();
    }
}
