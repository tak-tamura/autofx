package com.takuro_tamura.autofx.domain.backtest;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.Price;
import com.takuro_tamura.autofx.domain.model.value.ProtectionLevels;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class BacktestExecutionPolicy {

    /**
     * 足の始値が保護水準を越えているギャップを判定する。
     * 指定したSL/TP価格で約定できたとは仮定せず、取得可能な始値を決済価格にする。
     */
    public Optional<ExitDecision> evaluateGap(Order order, Candle candle) {
        if (!isOpenAndProtected(order)) {
            return Optional.empty();
        }

        final BigDecimal open = candle.getOpen().getValue();
        final ProtectionLevels levels = order.getProtectionLevels();
        if (order.getSide() == OrderSide.BUY) {
            if (open.compareTo(levels.stopPrice().getValue()) <= 0) {
                return Optional.of(new ExitDecision(candle.getOpen(), BacktestExitReason.STOP_LOSS));
            }
            if (open.compareTo(levels.takeProfitPrice().getValue()) >= 0) {
                return Optional.of(new ExitDecision(candle.getOpen(), BacktestExitReason.TAKE_PROFIT));
            }
        } else {
            if (open.compareTo(levels.stopPrice().getValue()) >= 0) {
                return Optional.of(new ExitDecision(candle.getOpen(), BacktestExitReason.STOP_LOSS));
            }
            if (open.compareTo(levels.takeProfitPrice().getValue()) <= 0) {
                return Optional.of(new ExitDecision(candle.getOpen(), BacktestExitReason.TAKE_PROFIT));
            }
        }
        return Optional.empty();
    }

    /**
     * 始値ギャップ判定後、足の高値・安値からSL/TPへの接触を判定する。
     * OHLCだけでは足内の到達順を復元できないため、両方に触れた場合は保守的にSLを優先する。
     */
    public Optional<ExitDecision> evaluateIntrabar(Order order, Candle candle) {
        if (!isOpenAndProtected(order)) {
            return Optional.empty();
        }

        final ProtectionLevels levels = order.getProtectionLevels();
        final boolean stopTouched;
        final boolean profitTouched;
        if (order.getSide() == OrderSide.BUY) {
            stopTouched = candle.getLow().getValue().compareTo(levels.stopPrice().getValue()) <= 0;
            profitTouched = candle.getHigh().getValue().compareTo(levels.takeProfitPrice().getValue()) >= 0;
        } else {
            stopTouched = candle.getHigh().getValue().compareTo(levels.stopPrice().getValue()) >= 0;
            profitTouched = candle.getLow().getValue().compareTo(levels.takeProfitPrice().getValue()) <= 0;
        }

        if (stopTouched && profitTouched) {
            // 下位足・tickがない状態で有利な順序を仮定しない。
            return Optional.of(new ExitDecision(
                levels.stopPrice(),
                BacktestExitReason.BOTH_TOUCHED_STOP_FIRST
            ));
        }
        if (stopTouched) {
            return Optional.of(new ExitDecision(levels.stopPrice(), BacktestExitReason.STOP_LOSS));
        }
        if (profitTouched) {
            return Optional.of(new ExitDecision(levels.takeProfitPrice(), BacktestExitReason.TAKE_PROFIT));
        }
        return Optional.empty();
    }

    private boolean isOpenAndProtected(Order order) {
        return order != null && !order.getStatus().isCompleted() && order.getProtectionLevels() != null;
    }

    public record ExitDecision(Price price, BacktestExitReason reason) {
    }
}
