package com.takuro_tamura.autofx.application.command;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.SettleType;
import com.takuro_tamura.autofx.infrastructure.websocket.response.ExecutionResponse;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
public class Execution {
    private final long orderId;
    private final CurrencyPair currencyPair;
    private final SettleType settleType;
    private final OrderSide side;
    private final int size;
    private final double price;
    private final LocalDateTime executionTime;

    public Execution(ExecutionResponse executionResponse) {
        this.orderId = executionResponse.getOrderId();
        this.currencyPair = executionResponse.getSymbol();
        this.settleType = executionResponse.getSettleType();
        this.side = executionResponse.getSide();
        this.size = executionResponse.getExecutionSize();
        this.price = executionResponse.getExecutionPrice();
        this.executionTime = LocalDateTime.ofInstant(executionResponse.getExecutionTimestamp(), ZoneId.of("Asia/Tokyo"));
    }
}
