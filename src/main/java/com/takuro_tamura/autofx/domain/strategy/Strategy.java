package com.takuro_tamura.autofx.domain.strategy;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;

import java.util.List;

public interface Strategy {
    TradeSignal checkTradeSignal(List<Candle> candles, int index);
}
