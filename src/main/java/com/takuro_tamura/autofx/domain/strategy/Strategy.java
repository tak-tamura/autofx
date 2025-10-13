package com.takuro_tamura.autofx.domain.strategy;

import com.takuro_tamura.autofx.domain.model.value.TradeSignal;

public interface Strategy {
    TradeSignal checkTradeSignal(double[] closePrices, int index);
}
