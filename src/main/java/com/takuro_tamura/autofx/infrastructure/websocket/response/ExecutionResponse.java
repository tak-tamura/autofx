package com.takuro_tamura.autofx.infrastructure.websocket.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.SettleType;
import lombok.Data;

import java.time.Instant;

@Data
public class ExecutionResponse {
    private String channel;
    private String amount;
    private long rootOrderId;
    private long orderId;
    private String clientOrderId;
    private long executionId;
    private CurrencyPair symbol;
    private SettleType settleType;
    private String orderType;
    private String executionType;
    private OrderSide side;
    private double executionPrice;
    private int executionSize;
    private long positionId;
    private String lossGain;
    private String settledSwap;
    private String fee;
    private String orderPrice;
    private String orderExecutedSize;
    private String orderSize;
    private String msgType;

    //@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant orderTimestamp;

    //@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant executionTimestamp;
}
