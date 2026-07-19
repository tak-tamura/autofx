package com.takuro_tamura.autofx.parametersearch.dataset;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GMOコインのKLine日付はJST 6:00に切り替わる。
 * 0:00〜5:59の足は暦上の前日をAPI日付として扱う。
 */
public final class GmoCoinApiDate {
    private static final int ROLLOVER_HOUR = 6;

    private GmoCoinApiDate() {
    }

    public static LocalDate fromCandleTime(LocalDateTime candleTime) {
        if (candleTime == null) {
            throw new IllegalArgumentException("Candle time is required");
        }
        return candleTime.minusHours(ROLLOVER_HOUR).toLocalDate();
    }
}
