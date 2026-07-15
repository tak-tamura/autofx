package com.takuro_tamura.autofx.infrastructure.external.response;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OpenPosition {
    private Long positionId;
    private CurrencyPair symbol;
    private BigDecimal size;
    private BigDecimal orderedSize;
}
