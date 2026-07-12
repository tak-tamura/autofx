package com.takuro_tamura.autofx.application.strategy;

import com.takuro_tamura.autofx.application.command.chart.GetChartCommand;
import com.takuro_tamura.autofx.domain.service.CandleService;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.domain.strategy.EmaCrossStrategy;
import com.takuro_tamura.autofx.domain.strategy.Strategy;
import com.takuro_tamura.autofx.domain.strategy.config.StrategyConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Strategy インスタンスを生成するファクトリー
 * Application層で設定値を組み立て、Domain層の純粋なStrategyを生成する
 */
@Component
@RequiredArgsConstructor
public class StrategyFactory {
    private final CandleService candleService;
    private final TradeConfigParameterService configService;

    /**
     * 現在の設定に基づいて EMA Crossover Strategy を生成
     * 毎回新しいインスタンスを作成し、最新の設定パラメータを反映する
     */
    public Strategy createEmaCrossStrategy() {
        final StrategyConfig config = new StrategyConfig(
            configService.getEmaPeriod1(),
            configService.getEmaPeriod2(),
            configService.getRsiPeriod(),
            configService.getMacdFastPeriod(),
            configService.getMacdSlowPeriod(),
            configService.getMacdSignalPeriod(),
            configService.getBBandsN(),
            configService.getBBandsK(),
            configService.getAdxPeriod(),
            configService.getAdxThreshold()
        );
        return new EmaCrossStrategy(candleService, config);
    }

    /**
     * GetChartCommand のパラメータに基づいて EMA Crossover Strategy を生成
     * チャート表示やバックテスト時に、画面で指定したパラメータを使用する
     * ADX Threshold のみ DB パラメータを参照
     */
    public Strategy createEmaCrossStrategy(GetChartCommand command) {
        final StrategyConfig config = new StrategyConfig(
            command.getEma().getPeriods()[0],         // emaPeriod1
            command.getEma().getPeriods()[1],         // emaPeriod2
            command.getRsi().getPeriod(),             // rsiPeriod
            command.getMacd().getInFastPeriod(),      // macdFastPeriod
            command.getMacd().getInSlowPeriod(),      // macdSlowPeriod
            command.getMacd().getInSignalPeriod(),    // macdSignalPeriod
            command.getBbands().getN(),               // bbandsN
            command.getBbands().getK(),               // bbandsK
            command.getAdx().getPeriod(),             // adxPeriod
            configService.getAdxThreshold()           // adxThreshold (DB参照)
        );
        return new EmaCrossStrategy(candleService, config);
    }
}
