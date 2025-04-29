package com.takuro_tamura.autofx.infrastructure.external.response;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import lombok.Data;

import java.time.Instant;

@Data
public class Ticker {
    private String ask;

    private String bid;

    private CurrencyPair symbol;

    private Instant timestamp;

    private FxStatus status;
}
