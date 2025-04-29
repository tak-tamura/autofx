package com.takuro_tamura.autofx.infrastructure.external.response;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.infrastructure.external.enums.ExecutionType;
import lombok.Data;

import java.time.Instant;

@Data
public class OrderResponse {
    private Long rootOrderId;
    private String clientOrderId;
    private Long orderId;
    private CurrencyPair symbol;
    private OrderSide side;
    private String orderType;
    private ExecutionType executionType;
    private String settleType;
    private Integer size;
    private Double price;
    private String status;
    private String cancelType;
    private String expiry;
    private Instant timestamp;
}
