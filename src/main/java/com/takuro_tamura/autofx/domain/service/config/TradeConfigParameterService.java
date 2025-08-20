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

    public boolean isBackTest() {
        return configParameterService.getBoolean("TRADE.BACK_TEST", false);
    }

    public TimeFrame getTargetTimeFrame() {
        final String timeFrame = configParameterService.getString("TRADE.TARGET_TIME_FRAME", "1h");
        return TimeFrame.fromLabel(timeFrame.trim().toLowerCase());
    }

    public int getMaxCandleNum() {
        return configParameterService.getInt("TRADE.MAX_CANDLE_NUM", 365);
    }

    public int getBuyPointThreshold() {
        return configParameterService.getInt("TRADE.BUY_POINT_THRESHOLD", 1);
    }

    public int getSellPointThreshold() {
        return configParameterService.getInt("TRADE.SELL_POINT_THRESHOLD", 1);
    }

    public BigDecimal getAvailableBalanceRate() {
        final Double availableBalanceRate = configParameterService.getDouble("TRADE.AVAILABLE_BALANCE_RATE", 0.8);
        return BigDecimal.valueOf(availableBalanceRate);
    }

    public BigDecimal getLeverage() {
        final Double leverage = configParameterService.getDouble("TRADE.LEVERAGE", 15.0);
        return BigDecimal.valueOf(leverage);
    }

    public BigDecimal getApiCost() {
        final Double apiCost = configParameterService.getDouble("TRADE.API_COST", 0.002);
        return BigDecimal.valueOf(apiCost);
    }

    public BigDecimal getStopLimit() {
        final Double stopLimit = configParameterService.getDouble("TRADE.STOP_LIMIT", 0.08);
        return BigDecimal.valueOf(stopLimit);
    }

    public BigDecimal getProfitLimit() {
        final Double profitLimit = configParameterService.getDouble("TRADE.PROFIT_LIMIT", 0.15);
        return BigDecimal.valueOf(profitLimit);
    }
}
