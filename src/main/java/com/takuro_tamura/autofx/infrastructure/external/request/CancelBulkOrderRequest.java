package com.takuro_tamura.autofx.infrastructure.external.request;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.model.value.SettleType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CancelBulkOrderRequest {
    private List<CurrencyPair> symbols;
    private OrderSide side;
    private SettleType settleType;
}
