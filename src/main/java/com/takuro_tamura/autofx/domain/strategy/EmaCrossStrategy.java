package com.takuro_tamura.autofx.domain.strategy;

import com.takuro_tamura.autofx.domain.indicator.BBands;
import com.takuro_tamura.autofx.domain.indicator.Ema;
import com.takuro_tamura.autofx.domain.indicator.Macd;
import com.takuro_tamura.autofx.domain.indicator.Rsi;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmaCrossStrategy implements Strategy {

    private final TradeConfigParameterService tradeConfigParameterService;

    public TradeSignal checkTradeSignal(double[] closePrices, int index) {
        final int[] emaPeriods = new int[]{
            tradeConfigParameterService.getEmaPeriod1(),
            tradeConfigParameterService.getEmaPeriod2()
        };
        final Ema ema = new Ema(emaPeriods, closePrices);

        final BBands bBands = new BBands(
            tradeConfigParameterService.getBBandsN(),
            tradeConfigParameterService.getBBandsK(),
            closePrices
        );

        final Rsi rsi = new Rsi(tradeConfigParameterService.getRsiPeriod(), closePrices);

        final Macd macd = new Macd(
            tradeConfigParameterService.getMacdFastPeriod(),
            tradeConfigParameterService.getMacdSlowPeriod(),
            tradeConfigParameterService.getMacdSignalPeriod(),
            closePrices
        );

        if (checkBuySignal(ema, bBands, rsi, macd, closePrices, index)) {
            return TradeSignal.BUY;
        } else if (checkSellSignal(ema, bBands, rsi, macd, closePrices, index)) {
            return TradeSignal.SELL;
        }
        return TradeSignal.NONE;
    }

    private boolean checkBuySignal(Ema ema, BBands bBands, Rsi rsi, Macd macd, double[] closePrices, int index) {
        if (!ema.shouldBuy(index)) {
            return false;
        }

        if (rsi.getValues()[index] <= 50.0) {
            return false;
        }

        if (macd.getMacdHist()[index] <= 0.0) {
            return false;
        }
        return closePrices[index] < bBands.getUp()[index];
    }

    private boolean checkSellSignal(Ema ema, BBands bBands, Rsi rsi, Macd macd, double[] closePrices, int index) {
        if (!ema.shouldSell(index)) {
            return false;
        }

        if (rsi.getValues()[index] >= 50.0) {
            return false;
        }

        if (macd.getMacdHist()[index] >= 0.0) {
            return false;
        }
        return closePrices[index] > bBands.getDown()[index];
    }
}
