package com.takuro_tamura.autofx.domain.model.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@RequiredArgsConstructor
public enum TimeFrame {
    MINUTE("1m", "1min"),
    MINUTE15("15m", "15min"),

    HOUR("1h", "1hour"),
    HOUR4("4h", "4hour"),
    DAY("1d", "1day"),
    WEEK("1w", "1week"),
    MONTH("1month", "1month"),
    ;

    @Getter
    @JsonValue
    private final String label;

    @Getter
    private final String fullLabel;

    @JsonCreator
    public static TimeFrame fromLabel(String label) {
        return Arrays.stream(TimeFrame.values())
            .filter(timeFrame -> timeFrame.label.equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid TimeFrame label:" + label));
    }

    @JsonValue
    public String toValue() {
        return this.label;
    }

    public LocalDateTime truncateTime(LocalDateTime original) {
        return switch (this) {
            case MINUTE -> original.truncatedTo(ChronoUnit.MINUTES);
            case MINUTE15 -> original.withMinute((original.getMinute() / 15) * 15)
                .withSecond(0).withNano(0);
            case HOUR -> original.truncatedTo(ChronoUnit.HOURS);
            case HOUR4 -> original.withHour((original.getHour() / 4) * 4)
                .withMinute(0).withSecond(0).withNano(0);
            case DAY -> original.truncatedTo(ChronoUnit.DAYS);
            case WEEK -> original.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
            case MONTH -> original.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        };
    }
}
