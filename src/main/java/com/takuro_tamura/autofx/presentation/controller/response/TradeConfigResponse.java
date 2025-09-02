package com.takuro_tamura.autofx.presentation.controller.response;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import lombok.Builder;

@Builder
public record TradeConfigResponse(
    CurrencyPair targetCurrencyPair,
    boolean backTest,
    TimeFrame targetTimeFrame,
    int maxCandleNum,
    int buyPointThreshold,
    int sellPointThreshold,
    double availableBalanceRate,
    double leverage,
    double apiCost,
    double stopLimit,
    double profitLimit
) {}
