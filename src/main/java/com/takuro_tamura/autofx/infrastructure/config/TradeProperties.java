package com.takuro_tamura.autofx.infrastructure.config;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Data
@ConfigurationProperties(prefix = "trade")
public class TradeProperties {
    private CurrencyPair targetPair;
    private TimeFrame targetTimeFrame;
    private boolean backTest;
    private BigDecimal availableBalanceRate;
    private int maxCandleNum;
    private BigDecimal stopLimit;
    private BigDecimal profitLimit;
    private int indicatorLimit;
    private int buyPointThreshold;
    private int sellPointThreshold;
    private BigDecimal apiCost;
    private BigDecimal leverage;
}
