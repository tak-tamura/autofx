package com.takuro_tamura.autofx.domain.strategy;

import com.takuro_tamura.autofx.domain.indicator.BBands;
import com.takuro_tamura.autofx.domain.indicator.Ema;
import com.takuro_tamura.autofx.domain.indicator.Macd;
import com.takuro_tamura.autofx.domain.indicator.Rsi;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.indicator.AdxCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmaCrossStrategy implements Strategy {

    private final CandleService candleService;
    private final TradeConfigParameterService tradeConfigParameterService;

    @Override
    public TradeSignal checkTradeSignal(List<Candle> candles, int index) {
        final double[] adxValues = AdxCalculator.calculateAdx(
            candles,
            tradeConfigParameterService.getAdxPeriod()
        );

        final var adxFilter = new AdxEntryFilter(tradeConfigParameterService.getAdxThreshold());
        if (!adxFilter.canEnter(adxValues)) {
            log.info("ADX filter not passed: ADX={}", adxValues[index]);
            return TradeSignal.NONE;
        }

        final double[] closePrices = candleService.extractClosePrices(candles);

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
        log.info("EMA Buy condition met");

        if (rsi.getValues()[index] <= 50.0) {
            log.info("RSI condition not met for Buy: RSI={} ", rsi.getValues()[index]);
            return false;
        }

        if (macd.getMacdHist()[index] <= 0.0) {
            log.info("MACD condition not met for Buy: MACD Hist={}", macd.getMacdHist()[index]);
            return false;
        }

        if (closePrices[index] >= bBands.getUp()[index]) {
            log.info("Bollinger Bands condition not met for Buy: Close Price={} >= Upper Band={}",
                closePrices[index], bBands.getUp()[index]);
            return false;
        } else {
            log.info("All Buy conditions met: Close Price={} < Upper Band={}",
                closePrices[index], bBands.getUp()[index]);
            return true;
        }
    }

    private boolean checkSellSignal(Ema ema, BBands bBands, Rsi rsi, Macd macd, double[] closePrices, int index) {
        if (!ema.shouldSell(index)) {
            return false;
        }
        log.info("EMA Sell condition met");

        if (rsi.getValues()[index] >= 50.0) {
            log.info("RSI condition not met for Sell: RSI={} ", rsi.getValues()[index]);
            return false;
        }

        if (macd.getMacdHist()[index] >= 0.0) {
            log.info("MACD condition not met for Sell: MACD Hist={}", macd.getMacdHist()[index]);
            return false;
        }

        if (closePrices[index] <= bBands.getDown()[index]) {
            log.info("Bollinger Bands condition not met for Sell: Close Price={} <= Lower Band={}",
                closePrices[index], bBands.getDown()[index]);
            return false;
        } else {
            log.info("All Sell conditions met: Close Price={} > Lower Band={}",
                closePrices[index], bBands.getDown()[index]);
            return true;
        }
    }
}
