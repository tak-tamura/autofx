package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.domain.indicator.*;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.parameter.TradeParameter;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.domain.service.TradeParameterService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.infrastructure.cache.CacheKey;
import com.takuro_tamura.autofx.infrastructure.cache.RedisCacheService;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PrivateApi;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PublicApi;
import com.takuro_tamura.autofx.infrastructure.external.response.Assets;
import com.takuro_tamura.autofx.infrastructure.external.response.Ticker;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradeApplicationService {
    private final static int MINIMUM_ORDER_QUANTITY = 10000;

    private final Logger log = LoggerFactory.getLogger(TradeApplicationService.class);
    private final CandleService candleService;
    private final OrderService orderService;
    private final TradeParameterService tradeParameterService;
    private final TradeConfigParameterService tradeConfigParameterService;
    private final CandleRepository candleRepository;
    private final OrderRepository orderRepository;
    private final PrivateApi privateApi;
    private final PublicApi publicApi;
    private final RedisCacheService redisCacheService;

    @Transactional
    public synchronized void trade() {
        if (tradeConfigParameterService.isBackTest()) {
            log.info("Back test mode is on, skip trading");
            return;
        }

        final Boolean tradeEnabled = redisCacheService.<Boolean>get(CacheKey.TRADE_ENABLED.getKey())
            .orElse(Boolean.TRUE);
        if (!tradeEnabled) {
            log.info("Trade is disabled");
            return;
        }

        final CurrencyPair targetPair = tradeConfigParameterService.getTargetCurrencyPair();
        final TimeFrame targetTimeFrame = tradeConfigParameterService.getTargetTimeFrame();
        log.info("Start trade, currency: {}, timeframe: {}",
            targetPair,
            targetTimeFrame
        );

        final List<Candle> candles = candleRepository.findAllWithLimit(
            targetPair,
            targetTimeFrame,
            tradeConfigParameterService.getMaxCandleNum()
        );

        final Optional<Order> lastOrder = orderRepository.findLatestByCurrencyPairWithLock(targetPair);
        final Candle latestCandle = candles.get(candles.size() - 1);
        if (orderService.shouldCloseOrder(lastOrder.orElse(null), latestCandle.getClose())) {
            lastOrder.ifPresent(orderService::closeOrder);
        }

        if (CollectionUtils.size(candles) < 2) {
            log.info("Not sufficient candles found({}), abort trading", CollectionUtils.size(candles));
            return;
        }

        final Optional<TradeParameter> parameterOptional = tradeParameterService.optimizeParameter(candles);
        final TradeParameter parameter;
        if (parameterOptional.isPresent()) {
            parameter = parameterOptional.get();
        } else {
            log.info("Cannot create trade parameter, abort trading");
            return;
        }

        log.info("Trade parameter: {}", parameter);

        final double[] closePrices = candleService.extractClosePrices(candles);
        final Indicators indicators = prepareIndicators(parameter, closePrices);

        final int buyPoint = checkBuySignal(indicators, parameter, latestCandle, closePrices);
        final int sellPoint = checkSellSignal(indicators, parameter, latestCandle, closePrices);

        if (buyPoint >= tradeConfigParameterService.getBuyPointThreshold()) {
            if (orderService.canOrder(lastOrder.orElse(null))) {
                makeOrder(OrderSide.BUY, targetPair);
            } else {
                lastOrder.ifPresent(order -> {
                    if (order.getSide() != OrderSide.BUY) {
                        orderService.closeOrder(order);
                    }
                });
            }
        } else if (sellPoint >= tradeConfigParameterService.getSellPointThreshold()) {
            if (orderService.canOrder(lastOrder.orElse(null))) {
                makeOrder(OrderSide.SELL, targetPair);
            } else {
                lastOrder.ifPresent(order -> {
                    if (order.getSide() != OrderSide.SELL) {
                        orderService.closeOrder(order);
                    }
                });
            }
        } else {
            log.info("Skip trading this time because of no trading signal");
        }
    }

    private int checkBuySignal(Indicators indicators, TradeParameter parameter, Candle candle, double[] closePrices) {
        int buyPoint = 0;
        if (parameter.getEma().isEnable()) {
            if (indicators.ema.shouldBuy()) {
                log.info("Got buy signal by EMA");
                buyPoint++;
            }
        }

        if (parameter.getBBands().isEnable()) {
            if (indicators.bBands.shouldBuy(closePrices)) {
                log.info("Got buy signal by BBands");
                buyPoint++;
            }
        }

        if (parameter.getIchimoku().isEnable()) {
            if (indicators.ichimoku.shouldBuy(
                candle.getHigh().getValue().doubleValue(),
                candle.getLow().getValue().doubleValue()
            )) {
                log.info("Got buy signal by IchimokuCloud");
                buyPoint++;
            }
        }

        if (parameter.getMacd().isEnable()) {
            if (indicators.macd.shouldBuy()) {
                log.info("Got buy signal by MACD");
                buyPoint++;
            }
        }

        if (parameter.getRsi().isEnable()) {
            if (indicators.rsi.shouldBuy(parameter.getRsi().getBuyThread())) {
                log.info("Got buy signal by RSI");
                buyPoint++;
            }
        }

        return buyPoint;
    }

    private int checkSellSignal(Indicators indicators, TradeParameter parameter, Candle candle, double[] closePrices) {
        int sellPoint = 0;
        if (parameter.getEma().isEnable()) {
            if (indicators.ema.shouldSell()) {
                log.info("Got sell signal by EMA");
                sellPoint++;
            }
        }

        if (parameter.getBBands().isEnable()) {
            if (indicators.bBands.shouldSell(closePrices)) {
                log.info("Got sell signal by BBands");
                sellPoint++;
            }
        }

        if (parameter.getIchimoku().isEnable()) {
            if (indicators.ichimoku.shouldSell(
                candle.getHigh().getValue().doubleValue(),
                candle.getLow().getValue().doubleValue()
            )) {
                log.info("Got sell signal by IchimokuCloud");
                sellPoint++;
            }
        }

        if (parameter.getMacd().isEnable()) {
            if (indicators.macd.shouldSell()) {
                log.info("Got sell signal by MACD");
                sellPoint++;
            }
        }

        if (parameter.getRsi().isEnable()) {
            if (indicators.rsi.shouldSell(parameter.getRsi().getBuyThread())) {
                log.info("Got sell signal by RSI");
                sellPoint++;
            }
        }

        return sellPoint;
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

    private void makeOrder(OrderSide type, CurrencyPair targetPair) {
        final int orderAmount = calculateOrderAmount(type, targetPair);
        log.info("Calculated order amount: {}", orderAmount);

        if (orderAmount < MINIMUM_ORDER_QUANTITY) {
            log.warn("Not enough available amount, cannot make new order");
            return;
        }

        orderService.makeOrder(targetPair, type, orderAmount);
    }

    /**
     * 取引する通貨の数量を計算する
     * @param side 取引種別
     * @return 取引数量
     */
    private int calculateOrderAmount(OrderSide side, CurrencyPair targetPair) {
        // 現在の価格を取得
        final Ticker ticker = publicApi.getTickers().stream()
            .filter(it -> it.getSymbol() == targetPair)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("cannot find ticker of " + targetPair.name()));

        // 資産残高を取得
        final Assets assets = privateApi.getAssets();
        log.info("Available asset amount: {}", assets.getAvailableAmount());

        // 取引可能金額 = 取引に使用する資産の割合 * 資産残高 * レバレッジ
        final BigDecimal availableAmount = tradeConfigParameterService.getAvailableBalanceRate()
            .multiply(BigDecimal.valueOf(assets.getAvailableAmount()))
            .multiply(tradeConfigParameterService.getLeverage());

        // 1通貨あたりの金額にAPIコストを加算
        final BigDecimal price = (side == OrderSide.BUY) ? new BigDecimal(ticker.getAsk()) : new BigDecimal(ticker.getBid());
        final BigDecimal priceWithApiCost = price.add(tradeConfigParameterService.getApiCost());

        // 取引可能金額 / 1通貨あたりの金額が取引数量
        return availableAmount.divide(priceWithApiCost, RoundingMode.HALF_DOWN).intValue();
    }

    private record Indicators(
        Ema ema,
        BBands bBands,
        IchimokuCloud ichimoku,
        Macd macd,
        Rsi rsi
    ) {}
}
