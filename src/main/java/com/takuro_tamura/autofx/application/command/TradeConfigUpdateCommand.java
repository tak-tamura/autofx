package com.takuro_tamura.autofx.application.command;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Min(1)
    private int emaPeriod1;

    @Min(1)
    private int emaPeriod2;

    @Min(1)
    @JsonProperty("bBandsN")
    private int bBandsN;

    @Min(0)
    @JsonProperty("bBandsK")
    private double bBandsK;

    @Min(1)
    private int rsiPeriod;

    @Min(1)
    private int macdFastPeriod;

    @Min(1)
    private int macdSlowPeriod;

    @Min(1)
    private int macdSignalPeriod;

    @Min(1)
    private int adxPeriod;

    @Min(0)
    private double adxThreshold;
}
