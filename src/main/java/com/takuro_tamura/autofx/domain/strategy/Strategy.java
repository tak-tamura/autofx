package com.takuro_tamura.autofx.domain.strategy;

import com.takuro_tamura.autofx.domain.model.entity.Candle;
import java.util.List;

public interface Strategy {
    /**
     * 対象データセットのインジケーターを一度だけ計算し、index単位で評価できる状態にする。
     */
    PreparedStrategy prepare(List<Candle> candles);
}
