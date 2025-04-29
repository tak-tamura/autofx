package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.dto.BBandsPerformance;
import com.takuro_tamura.autofx.domain.dto.EmaPerformance;
import com.takuro_tamura.autofx.domain.dto.MacdPerformance;
import com.takuro_tamura.autofx.domain.dto.RsiPerformance;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.parameter.TradeParameter;
import com.takuro_tamura.autofx.domain.service.indicator.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TradeParameterService {
    private final static int MAX_INDICATOR_NUM = 3; // TODO define in property file

    private final EmaService emaService;
    private final BBandsService bBandsService;
    private final IchimokuService ichimokuService;
    private final MacdService macdService;
    private final RsiService rsiService;

    public Optional<TradeParameter> optimizeParameter(List<Candle> candles) {
        final EmaPerformance emaPerformance = emaService.optimize(candles);
        final BBandsPerformance bBandsPerformance = bBandsService.optimize(candles);
        final double ichimokuPerformance = ichimokuService.optimize(candles);
        final MacdPerformance macdPerformance = macdService.optimize(candles);
        final RsiPerformance rsiPerformance = rsiService.optimize(candles);

        final Rank emaRank = new Rank(false, emaPerformance.performance());
        final Rank bBandsRank = new Rank(false, bBandsPerformance.performance());
        final Rank ichimokuRank = new Rank(false, ichimokuPerformance);
        final Rank macdRank = new Rank(false, macdPerformance.performance());
        final Rank rsiRank = new Rank(false, rsiPerformance.performance());
        final List<Rank> ranks = Stream.of(emaRank, bBandsRank, ichimokuRank, macdRank, rsiRank)
            .sorted(Comparator.comparing(Rank::getPerformance).reversed())
            .toList();

        boolean enable = false;
        int numIndicators = 0;
        for (Rank rank : ranks) {
            if (rank.getPerformance() > 0) {
                rank.setEnable(true);
                enable = true;
                numIndicators++;
            }

            if (numIndicators >= MAX_INDICATOR_NUM) {
                break;
            }
        }

        if (!enable) {
            return Optional.empty();
        }

        return Optional.of(new TradeParameter(
            new TradeParameter.Ema(emaRank.enable, emaPerformance.bestPeriod1(), emaPerformance.bestPeriod2()),
            new TradeParameter.BBands(bBandsRank.enable, bBandsPerformance.n(), bBandsPerformance.k()),
            new TradeParameter.Ichimoku(ichimokuRank.enable),
            new TradeParameter.Macd(
                macdRank.enable,
                macdPerformance.bestFastPeriod(),
                macdPerformance.bestSlowPeriod(),
                macdPerformance.bestSignalPeriod()
            ),
            new TradeParameter.Rsi(
                rsiRank.enable,
                rsiPerformance.period(),
                rsiPerformance.buyThread(),
                rsiPerformance.sellThread()
            )
        ));
    }

    @AllArgsConstructor
    @Getter
    private static class Rank {
        @Setter
        private boolean enable;
        private final double performance;
    }
}
