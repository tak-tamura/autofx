package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandleService {

    private final TradeConfigParameterService tradeConfigParameterService;

    private final CandleRepository candleRepository;

    public double[] extractClosePrices(List<Candle> candles) {
        return candles.stream()
            .map(candle -> candle.getClose().getValue())
            .mapToDouble(BigDecimal::doubleValue)
            .toArray();
    }

    public double[] getTR(int period) {
        final List<Candle> candles = candleRepository.findAllWithLimit(
            tradeConfigParameterService.getTargetCurrencyPair(),
            tradeConfigParameterService.getTargetTimeFrame(),
            period + 1
        );

        final double[] trValues = new double[period];

        for (int i = 1; i < candles.size(); i++) {
            final BigDecimal tr1 = candles.get(i).getHigh().subtract(candles.get(i).getLow()).getValue();
            final BigDecimal tr2 = candles.get(i).getHigh().subtract(candles.get(i - 1).getClose()).getValue().abs();
            final BigDecimal tr3 = candles.get(i).getLow().subtract(candles.get(i - 1).getClose()).getValue().abs();
            final BigDecimal tr = tr1.max(tr2).max(tr3);
            trValues[i - 1] = tr.doubleValue();
        }

        return trValues;
    }
}
