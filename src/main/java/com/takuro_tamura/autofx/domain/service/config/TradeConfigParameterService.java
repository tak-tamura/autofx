package com.takuro_tamura.autofx.domain.service.config;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TradeConfigParameterService {
    private final ConfigParameterService configParameterService;

    public CurrencyPair getTargetCurrencyPair() {
        final String pair = configParameterService.getString("TRADE.TARGET_PAIR", "USD_JPY");
        return CurrencyPair.valueOf(pair.trim().toUpperCase());
    }

    public boolean updateTargetCurrencyPair(CurrencyPair currencyPair) {
        return configParameterService.updateConfigParameter("TRADE.TARGET_PAIR", currencyPair.name());
    }

    public boolean isBackTest() {
        return configParameterService.getBoolean("TRADE.BACK_TEST", false);
    }

    public boolean updateBackTest(boolean isBackTest) {
        return configParameterService.updateConfigParameter("TRADE.BACK_TEST", String.valueOf(isBackTest));
    }

    public TimeFrame getTargetTimeFrame() {
        final String timeFrame = configParameterService.getString("TRADE.TARGET_TIME_FRAME", "1h");
        return TimeFrame.fromLabel(timeFrame.trim().toLowerCase());
    }

    public boolean updateTargetTimeFrame(TimeFrame timeFrame) {
        return configParameterService.updateConfigParameter("TRADE.TARGET_TIME_FRAME", timeFrame.getLabel());
    }

    public int getMaxCandleNum() {
        return configParameterService.getInt("TRADE.MAX_CANDLE_NUM", 365);
    }

    public boolean updateMaxCandleNum(int maxCandleNum) {
        return configParameterService.updateConfigParameter("TRADE.MAX_CANDLE_NUM", String.valueOf(maxCandleNum));
    }

    public int getBuyPointThreshold() {
        return configParameterService.getInt("TRADE.BUY_POINT_THRESHOLD", 1);
    }

    public boolean updateBuyPointThreshold(int buyPointThreshold) {
        return configParameterService.updateConfigParameter("TRADE.BUY_POINT_THRESHOLD", String.valueOf(buyPointThreshold));
    }

    public int getSellPointThreshold() {
        return configParameterService.getInt("TRADE.SELL_POINT_THRESHOLD", 1);
    }

    public boolean updateSellPointThreshold(int sellPointThreshold) {
        return configParameterService.updateConfigParameter("TRADE.SELL_POINT_THRESHOLD", String.valueOf(sellPointThreshold));
    }

    public BigDecimal getAvailableBalanceRate() {
        final Double availableBalanceRate = configParameterService.getDouble("TRADE.AVAILABLE_BALANCE_RATE", 0.8);
        return BigDecimal.valueOf(availableBalanceRate);
    }

    public boolean updateAvailableBalanceRate(double availableBalanceRate) {
        return configParameterService.updateConfigParameter("TRADE.AVAILABLE_BALANCE_RATE", String.valueOf(availableBalanceRate));
    }

    public BigDecimal getLeverage() {
        final Double leverage = configParameterService.getDouble("TRADE.LEVERAGE", 15.0);
        return BigDecimal.valueOf(leverage);
    }

    public boolean updateLeverage(double leverage) {
        return configParameterService.updateConfigParameter("TRADE.LEVERAGE", String.valueOf(leverage));
    }

    public BigDecimal getApiCost() {
        final Double apiCost = configParameterService.getDouble("TRADE.API_COST", 0.002);
        return BigDecimal.valueOf(apiCost);
    }

    public boolean updateApiCost(double apiCost) {
        return configParameterService.updateConfigParameter("TRADE.API_COST", String.valueOf(apiCost));
    }

    public BigDecimal getStopLimit() {
        final Double stopLimit = configParameterService.getDouble("TRADE.STOP_LIMIT", 0.08);
        return BigDecimal.valueOf(stopLimit);
    }

    public boolean updateStopLimit(double stopLimit) {
        return configParameterService.updateConfigParameter("TRADE.STOP_LIMIT", String.valueOf(stopLimit));
    }

    public BigDecimal getProfitLimit() {
        final Double profitLimit = configParameterService.getDouble("TRADE.PROFIT_LIMIT", 0.15);
        return BigDecimal.valueOf(profitLimit);
    }

    public boolean updateProfitLimit(double profitLimit) {
        return configParameterService.updateConfigParameter("TRADE.PROFIT_LIMIT", String.valueOf(profitLimit));
    }

    public int getAtrPeriod() {
        return configParameterService.getInt("TRADE.ATR_PERIOD", 14);
    }

    public boolean updateAtrPeriod(int atrPeriod) {
        return configParameterService.updateConfigParameter("TRADE.ATR_PERIOD", String.valueOf(atrPeriod));
    }
}
