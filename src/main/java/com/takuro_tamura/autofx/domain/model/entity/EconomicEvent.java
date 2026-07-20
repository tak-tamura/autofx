package com.takuro_tamura.autofx.domain.model.entity;

import lombok.Getter;
import org.springframework.lang.Nullable;

import java.time.ZonedDateTime;

@Getter
public class EconomicEvent {
    private Integer id;

    private String name;

    private ZonedDateTime eventTime;

    private int windowBeforeMinutes;

    private int windowAfterMinutes;

    private boolean enabled;

    public EconomicEvent(
        @Nullable Integer id,
        String name,
        ZonedDateTime eventTime,
        int windowBeforeMinutes,
        int windowAfterMinutes,
        boolean enabled
    ) {
        this.id = id;
        this.name = name;
        this.eventTime = eventTime;
        this.windowBeforeMinutes = windowBeforeMinutes;
        this.windowAfterMinutes = windowAfterMinutes;
        this.enabled = enabled;
    }
}
