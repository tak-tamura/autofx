package com.takuro_tamura.autofx.infrastructure.external.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.infrastructure.external.enums.ExecutionType;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
public class CloseOrderRequest {
    private CurrencyPair symbol;
    private OrderSide side;
    @JsonSerialize(using = IntegerToStringSerializer.class)
    private Integer size;
    private ExecutionType executionType;
    @JsonSerialize(using = DoubleToStringSerializer.class)
    private Double limitPrice;
    @JsonSerialize(using = DoubleToStringSerializer.class)
    private Double stopPrice;
    @JsonSerialize(using = DoubleToStringSerializer.class)
    private Double lowerBound;
    @JsonSerialize(using = DoubleToStringSerializer.class)
    private Double upperBound;

    private SettlePosition settlePosition;

    @Builder
    public CloseOrderRequest(
        @NonNull CurrencyPair currencyPair,
        @NonNull OrderSide side,
        @NonNull ExecutionType executionType,
        @NonNull Integer size,
        Double limitPrice,
        Double stopPrice,
        Double lowerBound,
        Double upperBound
    ) {
        this.symbol = currencyPair;
        this.side = side;
        this.size = size;
        this.executionType = executionType;
        this.limitPrice = limitPrice;
        this.stopPrice = stopPrice;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public static class SettlePosition {
        private Integer positionId;
        @JsonSerialize(using = IntegerToStringSerializer.class)
        private Integer size;
    }
}
