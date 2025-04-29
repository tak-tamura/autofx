package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.indicator.*;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.parameter.TradeParameter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BackTestService {
    private final Logger log = LoggerFactory.getLogger(BackTestService.class);
    private final CandleService candleService;
    private final OrderService orderService;
    private final TradeParameterService tradeParameterService;
    private final CandleRepository candleRepository;

    public List<Order> backTest(CurrencyPair currencyPair, TimeFrame timeFrame, int limit) {
        final List<Candle> candles = candleRepository.findAllWithLimit(currencyPair, timeFrame, limit);
        if (CollectionUtils.isEmpty(candles)) {
            return Collections.emptyList();
        }

        final Optional<TradeParameter> parameterOptional = tradeParameterService.optimizeParameter(candles);
        if (parameterOptional.isEmpty()) {
            return Collections.emptyList();
        }
        final TradeParameter parameter = parameterOptional.get();

        final double[] closePrices = candleService.extractClosePrices(candles);

        final Indicators indicators = prepareIndicators(parameter, closePrices);

        final List<Order> orders = new ArrayList<>();
        Order lastOrder;

        for (int i = 1; i < candles.size(); i++) {
            final Candle candle = candles.get(i);
            lastOrder = orders.size() > 0 ? orders.get(orders.size() - 1) : null;

            int buyPoint = checkBuySignal(indicators, parameter, i, candle, closePrices);
            int sellPoint = checkSellSignal(indicators, parameter, i, candle, closePrices);

            if (buyPoint > 0) {
                orderService.handleBackTestOrder(OrderSide.BUY, orders, lastOrder, candle);
            }

            if (sellPoint > 0) {
                orderService.handleBackTestOrder(OrderSide.SELL, orders, lastOrder, candle);
            }
        }

        return orders;
    }

    private Indicators prepareIndicators(TradeParameter parameter, double[] closePrices) {
        final Ema ema = parameter.getEma().isEnable() ? new Ema(
            new int[]{
                parameter.getEma().getPeriod1(),
                parameter.getEma().getPeriod2()
            },
            closePrices
        ) : null;

        final BBands bBands = parameter.getBBands().isEnable() ? new BBands(
            parameter.getBBands().getN(),
            parameter.getBBands().getK(),
            closePrices
        ) : null;

        final IchimokuCloud ichimoku = parameter.getIchimoku().isEnable() ? new IchimokuCloud(closePrices) : null;

        final Macd macd = parameter.getMacd().isEnable() ? new Macd(
            parameter.getMacd().getFastPeriod(),
            parameter.getMacd().getSlowPeriod(),
            parameter.getMacd().getSignalPeriod(),
            closePrices
        ) : null;

        final Rsi rsi = parameter.getRsi().isEnable() ? new Rsi(
            parameter.getRsi().getPeriod(),
            closePrices
        ) : null;

        return new Indicators(ema, bBands, ichimoku, macd, rsi);
    }

    private int checkBuySignal(Indicators indicators, TradeParameter parameter, int index, Candle candle, double[] closePrices) {
        int buyPoint = 0;
        if (parameter.getEma().isEnable() && index >= parameter.getEma().getPeriod1() && index >= parameter.getEma().getPeriod2()) {
            if (indicators.ema.shouldBuy(index)) {
                log.debug("Got buy signal by EMA");
                buyPoint++;
            }
        }

        if (parameter.getBBands().isEnable() && index >= parameter.getBBands().getN()) {
            if (indicators.bBands.shouldBuy(index, closePrices)) {
                log.debug("Got buy signal by BBands");
                buyPoint++;
            }
        }

        if (parameter.getIchimoku().isEnable()) {
            if (indicators.ichimoku.shouldBuy(
                index,
                candle.getHigh().getValue().doubleValue(),
                candle.getLow().getValue().doubleValue()
            )) {
                log.debug("Got buy signal by IchimokuCloud");
                buyPoint++;
            }
        }

        if (parameter.getMacd().isEnable()) {
            if (indicators.macd.shouldBuy(index)) {
                log.debug("Got buy signal by MACD");
                buyPoint++;
            }
        }

        if (parameter.getRsi().isEnable()) {
            if (indicators.rsi.shouldBuy(index, parameter.getRsi().getBuyThread())) {
                log.debug("Got buy signal by RSI");
                buyPoint++;
            }
        }

        return buyPoint;
    }

    private int checkSellSignal(Indicators indicators, TradeParameter parameter, int index, Candle candle, double[] closePrices) {
        int sellPoint = 0;
        if (parameter.getEma().isEnable() && index >= parameter.getEma().getPeriod1() && index >= parameter.getEma().getPeriod2()) {
            if (indicators.ema.shouldSell(index)) {
                log.debug("Got sell signal by EMA");
                sellPoint++;
            }
        }

        if (parameter.getBBands().isEnable() && index >= parameter.getBBands().getN()) {
            if (indicators.bBands.shouldSell(index, closePrices)) {
                log.debug("Got sell signal by BBands");
                sellPoint++;
            }
        }

        if (parameter.getIchimoku().isEnable()) {
            if (indicators.ichimoku.shouldSell(
                index,
                candle.getHigh().getValue().doubleValue(),
                candle.getLow().getValue().doubleValue()
            )) {
                log.debug("Got sell signal by IchimokuCloud");
                sellPoint++;
            }
        }

        if (parameter.getMacd().isEnable()) {
            if (indicators.macd.shouldSell(index)) {
                log.debug("Got sell signal by MACD");
                sellPoint++;
            }
        }

        if (parameter.getRsi().isEnable()) {
            if (indicators.rsi.shouldSell(index, parameter.getRsi().getSellThread())) {
                log.debug("Got sell signal by RSI");
                sellPoint++;
            }
        }

        return sellPoint;
    }

    private record Indicators(
        Ema ema,
        BBands bBands,
        IchimokuCloud ichimoku,
        Macd macd,
        Rsi rsi
    ) {}
}
