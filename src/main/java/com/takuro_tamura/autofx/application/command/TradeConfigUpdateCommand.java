package com.takuro_tamura.autofx.application.command;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TradeConfigUpdateCommand {
    private CurrencyPair targetCurrencyPair;

    private boolean backTest;

    private TimeFrame targetTimeFrame;

    @Min(1)
    private int maxCandleNum;

    @Min(1)
    private int buyPointThreshold;

    @Min(1)
    private int sellPointThreshold;

    @Min(0)
    private double availableBalanceRate;

    @Min(1)
    private double leverage;

    @Min(0)
    private double apiCost;

    @Min(0)
    private double stopLimit;

    @Min(0)
    private double profitLimit;

    @Min(1)
    private int atrPeriod;
}
