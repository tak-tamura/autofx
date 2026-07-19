package com.takuro_tamura.autofx.application.validator;

import com.takuro_tamura.autofx.application.strategy.StrategyFactory;
import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.TradeSignal;
import com.takuro_tamura.autofx.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * トレードシグナルの検証と判定
 * 戦略シグナル生成、注文判定、クローズ判定を統合
 */
@Component
@RequiredArgsConstructor
public class TradeSignalValidator {

    private final Logger log = LoggerFactory.getLogger(TradeSignalValidator.class);
    private final OrderService orderService;
    private final StrategyFactory strategyFactory;

    /**
     * 現在の最新キャンドルに基づいて注文をクローズすべきか判定
     *
     * @param lastOrder     直前の注文（null の場合はスキップ）
     * @param latestPrice   最新価格
     * @return true: クローズすべき
     */
    public boolean shouldCloseOrder(Order lastOrder, Price latestPrice) {
        if (lastOrder == null) {
            return false;
        }
        return orderService.shouldCloseOrder(lastOrder, latestPrice);
    }

    /**
     * 過去のキャンドル履歴からトレードシグナルを生成
     * 最新キャンドルの1つ前（確定済み）のデータを使用してシグナル判定
     *
     * @param candles キャンドルリスト
     * @return トレードシグナル（BUY / SELL）
     */
    public TradeSignal generateSignal(List<Candle> candles) {
        return strategyFactory.createEmaCrossStrategy()
            .prepare(candles)
            .checkTradeSignal(candles.size() - 2);
    }

    /**
     * 未決済ポジションが存在するか判定
     *
     * @param lastOrder 直前の注文（null の場合は未決済ポジションなし）
     * @return true: 未決済ポジションあり
     */
    public boolean hasOpenPosition(Order lastOrder) {
        return orderService.hasOpenPosition(lastOrder);
    }

    /**
     * 既存注文が逆向きシグナルの場合にクローズ対象か判定
     * シグナルが BUY なのに既存注文が SELL などの場合に true
     *
     * @param signal    生成されたシグナル
     * @param lastOrder 既存注文
     * @return true: クローズすべき逆向き注文
     */
    public boolean isOppositeSignal(TradeSignal signal, Order lastOrder) {
        if (lastOrder == null || signal == null) {
            return false;
        }

        if (signal == TradeSignal.BUY) {
            return lastOrder.getSide() == OrderSide.SELL;
        } else if (signal == TradeSignal.SELL) {
            return lastOrder.getSide() == OrderSide.BUY;
        }

        return false;
    }
}
