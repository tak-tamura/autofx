package com.takuro_tamura.autofx.infrastructure.external.adaptor;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.infrastructure.external.adapter.PrivateApi;
import com.takuro_tamura.autofx.infrastructure.external.enums.ExecutionType;
import com.takuro_tamura.autofx.infrastructure.external.request.CloseOrderRequest;
import com.takuro_tamura.autofx.infrastructure.external.request.OrderRequest;
import com.takuro_tamura.autofx.infrastructure.external.response.Assets;
import com.takuro_tamura.autofx.infrastructure.external.response.OrderResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Disabled
@SpringBootTest
public class PrivateApiTest {
    @Autowired
    private PrivateApi privateApi;

    @Test
    public void testGetAssets() {
        final Assets assets = privateApi.getAssets();
        System.out.println(assets);
    }

    @Test
    public void testOrder() {
        final OrderRequest request = OrderRequest.newMarketOrder()
            .side(OrderSide.BUY)
            .currencyPair(CurrencyPair.USD_JPY)
            .size(10000)
            .build();

        final OrderResponse response = privateApi.order(request);
        System.out.println(response);
    }

    @Test
    public void testCloseOrder() {
        final CloseOrderRequest request = CloseOrderRequest.builder()
            .side(OrderSide.SELL)
            .currencyPair(CurrencyPair.USD_JPY)
            .size(10000)
            .executionType(ExecutionType.MARKET)
            .build();

        final List<OrderResponse> response = privateApi.closeOrder(request);
        System.out.println(response);
    }

    @Test
    public void testIssueWsToken() {
        final String token = privateApi.issueWsToken();
        System.out.println("token: " + token);
    }

    @Test
    public void testExtendWsToken() {
        privateApi.extendWsToken("s1lfJnOqJ72bF7_gvk0OqZ9hgj6TTKAo75C5jhJRkH0Dygt5PNUD-y6dmSN2");
    }
}
