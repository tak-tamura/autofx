package com.takuro_tamura.autofx.infrastructure.external.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.infrastructure.external.enums.ExecutionType;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderRequest {
    private CurrencyPair symbol;
    private OrderSide side;
    @JsonSerialize(using = IntegerToStringSerializer.class)
    private Integer size;
    private ExecutionType executionType;
    @JsonSerialize(using = IntegerToStringSerializer.class)
    private Integer limitPrice;
    @JsonSerialize(using = IntegerToStringSerializer.class)
    private Integer stopPrice;
    @JsonSerialize(using = IntegerToStringSerializer.class)
    private Integer lowerBound;
    @JsonSerialize(using = IntegerToStringSerializer.class)
    private Integer upperBound;

    @Builder(builderMethodName = "newMarketOrder")
    public OrderRequest(
        @NonNull CurrencyPair currencyPair,
        @NonNull OrderSide side,
        @NonNull Integer size,
        Integer lowerBound,
        Integer upperBound
    ) {
        this.symbol = currencyPair;
        this.side = side;
        this.size = size;
        this.executionType = ExecutionType.MARKET;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }
}
