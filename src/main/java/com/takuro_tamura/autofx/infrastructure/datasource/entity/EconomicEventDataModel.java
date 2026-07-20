package com.takuro_tamura.autofx.infrastructure.datasource.entity;

import com.takuro_tamura.autofx.domain.model.entity.EconomicEvent;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;

@Data
public class EconomicEventDataModel {
    private Integer id;
    private String name;
    private Instant eventTimeUtc;
    private Integer windowBeforeMin;
    private Integer windowAfterMin;
    private Boolean enabled;

    public EconomicEventDataModel() {}

    public EconomicEventDataModel(EconomicEvent economicEvent) {
        this.id = economicEvent.getId();
        this.name = economicEvent.getName();
        this.eventTimeUtc = economicEvent.getEventTime().toInstant();
        this.windowBeforeMin = economicEvent.getWindowBeforeMinutes();
        this.windowAfterMin = economicEvent.getWindowAfterMinutes();
        this.enabled = economicEvent.isEnabled();
    }

    public EconomicEvent toModel() {
        return new EconomicEvent(
            this.id,
            this.name,
            this.eventTimeUtc.atZone(ZoneId.of("Asia/Tokyo")),
            this.windowBeforeMin,
            this.windowAfterMin,
            this.enabled
        );
    }
}
