package com.takuro_tamura.autofx.application.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.TimeFrame;
import lombok.Data;

@Data
public class CandleImportCommand {
    private CurrencyPair currencyPair;
    private TimeFrame timeFrame;
    private String fromDate;
    private String toDate;

    @JsonProperty(defaultValue = "false")
    private boolean truncate;
}
