package com.takuro_tamura.autofx.application;

import com.takuro_tamura.autofx.application.command.TradeConfigUpdateCommand;
import com.takuro_tamura.autofx.domain.service.config.TradeConfigParameterService;
import com.takuro_tamura.autofx.presentation.controller.response.TradeConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeConfigParameterApplicationService {
    private final TradeConfigParameterService tradeConfigParameterService;

    public TradeConfigResponse getTradeConfig() {
        return TradeConfigResponse.builder()
            .targetCurrencyPair(tradeConfigParameterService.getTargetCurrencyPair())
            .backTest(tradeConfigParameterService.isBackTest())
            .targetTimeFrame(tradeConfigParameterService.getTargetTimeFrame())
            .maxCandleNum(tradeConfigParameterService.getMaxCandleNum())
            .buyPointThreshold(tradeConfigParameterService.getBuyPointThreshold())
            .sellPointThreshold(tradeConfigParameterService.getSellPointThreshold())
            .availableBalanceRate(tradeConfigParameterService.getAvailableBalanceRate().doubleValue())
            .leverage(tradeConfigParameterService.getLeverage().doubleValue())
            .apiCost(tradeConfigParameterService.getApiCost().doubleValue())
            .stopLimit(tradeConfigParameterService.getStopLimit().doubleValue())
            .profitLimit(tradeConfigParameterService.getProfitLimit().doubleValue())
            .atrPeriod(tradeConfigParameterService.getAtrPeriod())
            .emaPeriod1(tradeConfigParameterService.getEmaPeriod1())
            .emaPeriod2(tradeConfigParameterService.getEmaPeriod2())
            .bBandsN(tradeConfigParameterService.getBBandsN())
            .bBandsK(tradeConfigParameterService.getBBandsK())
            .rsiPeriod(tradeConfigParameterService.getRsiPeriod())
            .macdFastPeriod(tradeConfigParameterService.getMacdFastPeriod())
            .macdSlowPeriod(tradeConfigParameterService.getMacdSlowPeriod())
            .macdSignalPeriod(tradeConfigParameterService.getMacdSignalPeriod())
            .adxPeriod(tradeConfigParameterService.getAdxPeriod())
            .adxThreshold(tradeConfigParameterService.getAdxThreshold())
            .build();
    }

    public void updateTradeConfig(TradeConfigUpdateCommand command) {
        if (!tradeConfigParameterService.updateTargetCurrencyPair(command.getTargetCurrencyPair())) {
            log.warn("Failed to update target currency pair");
        }
        if (!tradeConfigParameterService.updateBackTest(command.isBackTest())) {
            log.warn("Failed to update back test mode");
        }
        if (!tradeConfigParameterService.updateTargetTimeFrame(command.getTargetTimeFrame())) {
            log.warn("Failed to update target time frame");
        }
        if (!tradeConfigParameterService.updateMaxCandleNum(command.getMaxCandleNum())) {
            log.warn("Failed to update max candle number");
        }
        if (!tradeConfigParameterService.updateBuyPointThreshold(command.getBuyPointThreshold())) {
            log.warn("Failed to update buy point threshold");
        }
        if (!tradeConfigParameterService.updateSellPointThreshold(command.getSellPointThreshold())) {
            log.warn("Failed to update sell point threshold");
        }
        if (!tradeConfigParameterService.updateAvailableBalanceRate(command.getAvailableBalanceRate())) {
            log.warn("Failed to update available balance rate");
        }
        if (!tradeConfigParameterService.updateLeverage(command.getLeverage())) {
            log.warn("Failed to update leverage");
        }
        if (!tradeConfigParameterService.updateApiCost(command.getApiCost())) {
            log.warn("Failed to update API cost");
        }
        if (!tradeConfigParameterService.updateStopLimit(command.getStopLimit())) {
            log.warn("Failed to update stop limit");
        }
        if (!tradeConfigParameterService.updateProfitLimit(command.getProfitLimit())) {
            log.warn("Failed to update profit limit");
        }
        if (!tradeConfigParameterService.updateAtrPeriod(command.getAtrPeriod())) {
            log.warn("Failed to update ATR period");
        }
        if (!tradeConfigParameterService.updateEmaPeriod1(command.getEmaPeriod1())) {
            log.warn("Failed to update EMA period 1");
        }
        if (!tradeConfigParameterService.updateEmaPeriod2(command.getEmaPeriod2())) {
            log.warn("Failed to update EMA period 2");
        }
        if (!tradeConfigParameterService.updateBBandsN(command.getBBandsN())) {
            log.warn("Failed to update Bollinger Bands N");
        }
        if (!tradeConfigParameterService.updateBBandsK(command.getBBandsK())) {
            log.warn("Failed to update Bollinger Bands K");
        }
        if (!tradeConfigParameterService.updateRsiPeriod(command.getRsiPeriod())) {
            log.warn("Failed to update RSI period");
        }
        if (!tradeConfigParameterService.updateMacdFastPeriod(command.getMacdFastPeriod())) {
            log.warn("Failed to update MACD fast period");
        }
        if (!tradeConfigParameterService.updateMacdSlowPeriod(command.getMacdSlowPeriod())) {
            log.warn("Failed to update MACD slow period");
        }
        if (!tradeConfigParameterService.updateMacdSignalPeriod(command.getMacdSignalPeriod())) {
            log.warn("Failed to update MACD signal period");
        }
        if (!tradeConfigParameterService.updateAdxPeriod(command.getAdxPeriod())) {
            log.warn("Failed to update ADX period");
        }
        if (!tradeConfigParameterService.updateAdxThreshold(command.getAdxThreshold())) {
            log.warn("Failed to update ADX threshold");
        }
    }
}
