package com.takuro_tamura.autofx.domain.service;

import com.takuro_tamura.autofx.domain.backtest.BacktestAssumptions;
import com.takuro_tamura.autofx.domain.backtest.BacktestExecutionPolicy;
import com.takuro_tamura.autofx.domain.backtest.BacktestExitReason;
import com.takuro_tamura.autofx.domain.backtest.BacktestResult;
import com.takuro_tamura.autofx.domain.backtest.BacktestRiskParameters;
import com.takuro_tamura.autofx.domain.backtest.BacktestTrade;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.CandleRepository;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.service.indicator.AtrCalculator;
import com.takuro_tamura.autofx.domain.strategy.PreparedStrategy;
import com.takuro_tamura.autofx.domain.strategy.Strategy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackTestService {
    private final Logger log = LoggerFactory.getLogger(BackTestService.class);
    private final CandleService candleService;
    private final OrderService orderService;
    private final CandleRepository candleRepository;
    private final TradeConfigParameterService tradeConfigParameterService;
    private final BacktestExecutionPolicy executionPolicy;

    public double getBackTestProfitLoss(CurrencyPair currencyPair, TimeFrame timeFrame, int limit, Strategy strategy) {
        return run(currencyPair, timeFrame, limit, strategy).totalProfit().doubleValue();
    }

    public List<Order> backTest(CurrencyPair currencyPair, TimeFrame timeFrame, int limit, Strategy strategy) {
        return run(currencyPair, timeFrame, limit, strategy).orders();
    }

    public BacktestResult run(CurrencyPair currencyPair, TimeFrame timeFrame, int limit, Strategy strategy) {
        final List<Candle> candles = candleRepository.findAllWithLimit(currencyPair, timeFrame, limit);
        if (CollectionUtils.isEmpty(candles)) {
            return new BacktestResult(Collections.emptyList(), BacktestAssumptions.current());
        }

        final BacktestRiskParameters riskParameters = new BacktestRiskParameters(
            tradeConfigParameterService.getAtrPeriod(),
            tradeConfigParameterService.getStopLimit(),
            tradeConfigParameterService.getProfitLimit()
        );

        return run(candles, strategy, riskParameters);
    }

    /**
     * 呼び出し側が固定したデータとリスク条件でバックテストを実行する。
     * パラメータ探索ではDBを参照しないこの入口を使い、実行条件の再現性を保つ。
     */
    public BacktestResult run(
        List<Candle> candles,
        Strategy strategy,
        BacktestRiskParameters riskParameters
    ) {
        if (candles == null || strategy == null || riskParameters == null) {
            throw new IllegalArgumentException("Candles, strategy, and backtest risk parameters are required");
        }
        if (candles.isEmpty()) {
            return new BacktestResult(Collections.emptyList(), BacktestAssumptions.current());
        }

        final List<BacktestTrade> trades = new ArrayList<>();
        // 全足で共有するインジケーターをループ前に一度だけ計算する。
        final PreparedStrategy preparedStrategy = strategy.prepare(candles);
        // 確定足で検出したシグナルを次足始値まで持ち越し、判定足終値での先回り約定を防ぐ。
        PendingAction pendingAction = null;

        /*
         * 各足は次の時系列で処理する。
         * 1. 前足から保有しているポジションの始値ギャップ
         * 2. 前足確定後に予約した新規・反対シグナル注文
         * 3. 当該足の高値・安値による足内決済
         * 4. 当該足確定後のシグナル判定（約定は次足）
         */
        for (int i = 1; i < candles.size(); i++) {
            final Candle candle = candles.get(i);
            Order openOrder = lastOpenOrder(trades);

            if (openOrder != null) {
                // SL/TPを越えて始まった場合、到達価格ではなく実際に取得可能な始値で決済する。
                executionPolicy.evaluateGap(openOrder, candle)
                    .ifPresent(decision -> closeTrade(trades, candle, decision.price(), decision.reason()));
            }

            openOrder = lastOpenOrder(trades);
            if (pendingAction != null) {
                if (pendingAction.type() == PendingActionType.CLOSE) {
                    // 反対シグナルでは既存ポジションの決済だけを行い、同時に反転売買はしない。
                    if (openOrder != null && openOrder.getSide() != pendingAction.side()) {
                        closeTrade(trades, candle, candle.getOpen(), BacktestExitReason.OPPOSITE_SIGNAL);
                    }
                } else if (openOrder == null) {
                    // 約定価格は次足始値、保護水準のATRはシグナル判定時点の値を使用する。
                    final Order openedOrder = orderService.createBackTestOrder(
                        pendingAction.side(),
                        candle,
                        candle.getOpen()
                    );
                    openedOrder.fixProtectionLevels(orderService.createProtectionLevels(
                        openedOrder,
                        pendingAction.atr(),
                        riskParameters.stopMultiplier(),
                        riskParameters.profitMultiplier()
                    ));
                    trades.add(new BacktestTrade(openedOrder, pendingAction.signalDatetime(), null));
                    log.debug(
                        "BackTest entry filled at {}, signalAt: {}, side: {}, price: {}, ATR: {}, stopPrice: {}, takeProfitPrice: {}",
                        candle.getTime(), pendingAction.signalDatetime(), openedOrder.getSide(),
                        openedOrder.getFillPrice(), openedOrder.getProtectionLevels().entryAtr(),
                        openedOrder.getProtectionLevels().stopPrice(),
                        openedOrder.getProtectionLevels().takeProfitPrice()
                    );
                }
                pendingAction = null;
            }

            openOrder = lastOpenOrder(trades);
            if (openOrder != null) {
                executionPolicy.evaluateIntrabar(openOrder, candle)
                    .ifPresent(decision -> closeTrade(trades, candle, decision.price(), decision.reason()));
            }

            if (i < candles.size() - 1) {
                // 次足が存在しない最終足のシグナルは、約定不能なので評価対象にしない。
                final TradeSignal signal = preparedStrategy.checkTradeSignal(i);
                pendingAction = createPendingAction(signal, trades, candle, candles, i, riskParameters);
            }
        }

        final Order finalOpenOrder = lastOpenOrder(trades);
        if (finalOpenOrder != null) {
            // 集計結果に含み損益を残さないよう、データ末尾では通常決済と区別して強制決済する。
            final Candle finalCandle = candles.get(candles.size() - 1);
            closeTrade(trades, finalCandle, finalCandle.getClose(), BacktestExitReason.END_OF_DATA);
        }

        return new BacktestResult(trades, BacktestAssumptions.current());
    }

    private PendingAction createPendingAction(
        TradeSignal signal,
        List<BacktestTrade> trades,
        Candle candle,
        List<Candle> candles,
        int evaluationIndex,
        BacktestRiskParameters riskParameters
    ) {
        if (signal == TradeSignal.NONE) {
            return null;
        }

        final OrderSide side = signal == TradeSignal.BUY ? OrderSide.BUY : OrderSide.SELL;
        final Order openOrder = lastOpenOrder(trades);
        if (openOrder != null) {
            // 最大1ポジションを維持する。同方向シグナルは無視し、反対方向だけ次足決済を予約する。
            return openOrder.getSide() != side
                ? new PendingAction(PendingActionType.CLOSE, side, candle.getTime(), null)
                : null;
        }
        if (evaluationIndex < riskParameters.atrPeriod()) {
            log.debug(
                "Skip BackTest entry at {} because ATR({}) is not available at index {}",
                candle.getTime(),
                riskParameters.atrPeriod(),
                evaluationIndex
            );
            return null;
        }

        // 約定する次足をATR計算へ含めず、シグナル時点で利用可能なデータだけを使う。
        final BigDecimal atr = AtrCalculator.calculate(
            candles,
            evaluationIndex,
            riskParameters.atrPeriod()
        );
        return new PendingAction(PendingActionType.OPEN, side, candle.getTime(), atr);
    }

    private Order lastOpenOrder(List<BacktestTrade> trades) {
        if (trades.isEmpty()) {
            return null;
        }
        final Order lastOrder = trades.get(trades.size() - 1).order();
        return !lastOrder.getStatus().isCompleted() ? lastOrder : null;
    }

    private void closeTrade(
        List<BacktestTrade> trades,
        Candle candle,
        Price price,
        BacktestExitReason reason
    ) {
        final int lastIndex = trades.size() - 1;
        final BacktestTrade trade = trades.get(lastIndex);
        trade.order().close(candle.getTime(), price);
        trades.set(lastIndex, trade.withExitReason(reason));
        log.debug("BackTest close at {}, side: {}, price: {}, reason: {}, profit: {}",
            candle.getTime(), trade.order().getSide(), price, reason, trade.order().calculateProfit());
    }

    private enum PendingActionType { OPEN, CLOSE }

    private record PendingAction(
        PendingActionType type,
        OrderSide side,
        java.time.LocalDateTime signalDatetime,
        BigDecimal atr
    ) {
    }

}
