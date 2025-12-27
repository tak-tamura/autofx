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

    public int getEmaPeriod1() {
        return configParameterService.getInt("TRADE.EMA_PERIOD1", 8);
    }

    public boolean updateEmaPeriod1(int emaPeriod1) {
        return configParameterService.updateConfigParameter("TRADE.EMA_PERIOD1", String.valueOf(emaPeriod1));
    }

    public int getEmaPeriod2() {
        return configParameterService.getInt("TRADE.EMA_PERIOD2", 21);
    }

    public boolean updateEmaPeriod2(int emaPeriod2) {
        return configParameterService.updateConfigParameter("TRADE.EMA_PERIOD2", String.valueOf(emaPeriod2));
    }

    public int getBBandsN() {
        return configParameterService.getInt("TRADE.BBANDS_N", 20);
    }

    public boolean updateBBandsN(int bBandsN) {
        return configParameterService.updateConfigParameter("TRADE.BBANDS_N", String.valueOf(bBandsN));
    }

    public double getBBandsK() {
        return configParameterService.getDouble("TRADE.BBANDS_K", 2.0);
    }

    public boolean updateBBandsK(double bBandsK) {
        return configParameterService.updateConfigParameter("TRADE.BBANDS_K", String.valueOf(bBandsK));
    }

    public int getRsiPeriod() {
        return configParameterService.getInt("TRADE.RSI_PERIOD", 14);
    }

    public boolean updateRsiPeriod(int rsiPeriod) {
        return configParameterService.updateConfigParameter("TRADE.RSI_PERIOD", String.valueOf(rsiPeriod));
    }

    public int getMacdFastPeriod() {
        return configParameterService.getInt("TRADE.MACD_FAST_PERIOD", 12);
    }

    public boolean updateMacdFastPeriod(int macdFastPeriod) {
        return configParameterService.updateConfigParameter("TRADE.MACD_FAST_PERIOD", String.valueOf(macdFastPeriod));
    }

    public int getMacdSlowPeriod() {
        return configParameterService.getInt("TRADE.MACD_SLOW_PERIOD", 26);
    }

    public boolean updateMacdSlowPeriod(int macdSlowPeriod) {
        return configParameterService.updateConfigParameter("TRADE.MACD_SLOW_PERIOD", String.valueOf(macdSlowPeriod));
    }

    public int getMacdSignalPeriod() {
        return configParameterService.getInt("TRADE.MACD_SIGNAL_PERIOD", 9);
    }

    public boolean updateMacdSignalPeriod(int macdSignalPeriod) {
        return configParameterService.updateConfigParameter("TRADE.MACD_SIGNAL_PERIOD", String.valueOf(macdSignalPeriod));
    }

    public int getAdxPeriod() {
        return configParameterService.getInt("TRADE.ADX_PERIOD", 14);
    }

    public boolean updateAdxPeriod(int adxPeriod) {
        return configParameterService.updateConfigParameter("TRADE.ADX_PERIOD", String.valueOf(adxPeriod));
    }

    public double getAdxThreshold() {
        return configParameterService.getDouble("TRADE.ADX_THRESHOLD", 20.0);
    }

    public boolean updateAdxThreshold(double adxThreshold) {
        return configParameterService.updateConfigParameter("TRADE.ADX_THRESHOLD", String.valueOf(adxThreshold));
    }
}
