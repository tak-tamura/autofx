package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.BackTestService;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.OrderService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.strategy.Strategy;
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
    private final TradeConfigParameterService tradeConfigParameterService;
    private final BackTestService backTestService;
    private final Strategy strategy;
    private final CandleRepository candleRepository;
    private final OrderRepository orderRepository;
    private final PrivateApi privateApi;
    private final PublicApi publicApi;
    private final RedisCacheService redisCacheService;

    @Transactional
    public void trade() {
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

        final double[] closePrices = candleService.extractClosePrices(candles);

        final double backTestResult = backTestService.getBackTestProfitLoss(
            targetPair,
            targetTimeFrame,
            tradeConfigParameterService.getMaxCandleNum()
        );

        if (backTestResult <= 0.0) {
            log.info("Back test result is no profit({}), abort trading", backTestResult);
            return;
        }

        final TradeSignal signal = strategy.checkTradeSignal(closePrices, closePrices.length - 2);

        if (signal == TradeSignal.BUY) {
            if (orderService.canMakeNewOrder(lastOrder.orElse(null))) {
                makeOrder(OrderSide.BUY, targetPair);
            } else {
                lastOrder.ifPresent(order -> {
                    if (order.getSide() != OrderSide.BUY) {
                        orderService.closeOrder(order);
                    }
                });
            }
        } else if (signal == TradeSignal.SELL) {
            if (orderService.canMakeNewOrder(lastOrder.orElse(null))) {
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
}
