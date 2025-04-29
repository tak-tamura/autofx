package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CandleService {
    public double[] extractClosePrices(List<Candle> candles) {
        return candles.stream()
            .map(candle -> candle.getClose().getValue())
            .mapToDouble(BigDecimal::doubleValue)
            .toArray();
    }
}
