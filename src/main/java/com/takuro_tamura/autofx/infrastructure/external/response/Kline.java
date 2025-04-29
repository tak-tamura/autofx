package com.takuro_tamura.autofx.infrastructure.external.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
public class Kline {
    private long openTime;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;

    public LocalDateTime getOpenTimeAsLocalDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(openTime), ZoneId.of("Asia/Tokyo"));
    }
}
