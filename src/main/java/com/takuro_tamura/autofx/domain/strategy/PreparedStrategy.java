package com.takuro_tamura.autofx.domain.strategy;

import com.takuro_tamura.autofx.domain.model.value.TradeSignal;

/**
 * 1つのローソク足データセットに対してインジケーター計算を完了したStrategy。
 * 各indexの評価では計算済みの値だけを参照する。
 */
@FunctionalInterface
public interface PreparedStrategy {
    TradeSignal checkTradeSignal(int index);
}
