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

    public List<Candle> getLatestCandles(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Candle limit must be greater than zero");
        }
        return candleRepository.findAllWithLimit(
            tradeConfigParameterService.getTargetCurrencyPair(),
            tradeConfigParameterService.getTargetTimeFrame(),
            limit
        );
    }
}
