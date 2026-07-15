package com.takuro_tamura.autofx.domain.model.value;

import java.math.BigDecimal;

public record ProtectionLevels(
    BigDecimal entryAtr,
    Price stopPrice,
    Price takeProfitPrice
) {
    public ProtectionLevels {
        if (entryAtr == null || entryAtr.signum() <= 0) {
            throw new IllegalArgumentException("Entry ATR must be greater than zero");
        }
        if (stopPrice == null || stopPrice.getValue().signum() <= 0) {
            throw new IllegalArgumentException("Stop price must be greater than zero");
        }
        if (takeProfitPrice == null || takeProfitPrice.getValue().signum() <= 0) {
            throw new IllegalArgumentException("Take-profit price must be greater than zero");
        }
    }
}
