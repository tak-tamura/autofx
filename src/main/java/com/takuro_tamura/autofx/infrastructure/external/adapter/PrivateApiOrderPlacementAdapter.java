package com.takuro_tamura.autofx.infrastructure.external.adapter;

import com.takuro_tamura.autofx.domain.exception.OrderPlacementException;
import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.domain.model.value.OrderSide;
import com.takuro_tamura.autofx.domain.service.port.OrderPlacementPort;
import com.takuro_tamura.autofx.infrastructure.external.enums.ExecutionType;
import com.takuro_tamura.autofx.infrastructure.external.request.CloseOrderRequest;
import com.takuro_tamura.autofx.infrastructure.external.request.OrderRequest;
import com.takuro_tamura.autofx.infrastructure.external.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PrivateApiOrderPlacementAdapter implements OrderPlacementPort {
    private final Logger log = LoggerFactory.getLogger(PrivateApiOrderPlacementAdapter.class);
    private final PrivateApi privateApi;
    
    @Override
    public Long submitMarketOrder(CurrencyPair currencyPair, OrderSide side, int size) {
        final OrderRequest request = OrderRequest.newMarketOrder()
            .currencyPair(currencyPair)
            .side(side)
            .size(size)
            .build();
        log.info("Submitting market order: {}", request);
        
        try {
            final OrderResponse response = privateApi.order(request);
            log.info("Market order submitted successfully, order id: {}", response.getOrderId());
            return response.getOrderId();
        } catch (Exception e) {
            log.error("Failed to submit market order", e);
            throw new OrderPlacementException("Failed to submit market order", e);
        }
    }
    
    @Override
    public Long submitMarketCloseOrder(Order order) {
        final CloseOrderRequest request = CloseOrderRequest.builder()
            .currencyPair(order.getCurrencyPair())
            .executionType(ExecutionType.MARKET)
            .side(OrderSide.getCloseSide(order.getSide()))
            .size(order.getSize())
            .build();
        log.info("Submitting market close order: {}", request);
        
        try {
            final OrderResponse response = privateApi.closeOrder(request).get(0);
            log.info("Market close order submitted successfully, order id: {}", response.getOrderId());
            return response.getOrderId();
        } catch (Exception e) {
            log.error("Failed to submit market close order", e);
            throw new OrderPlacementException("Failed to submit market close order", e);
        }
    }
    
    @Override
    public List<Long> submitOcoOrder(Order order, double stopPrice, double limitPrice) {
        final CloseOrderRequest request = CloseOrderRequest.builder()
            .currencyPair(order.getCurrencyPair())
            .executionType(ExecutionType.OCO)
            .side(OrderSide.getCloseSide(order.getSide()))
            .size(order.getSize())
            .stopPrice(stopPrice)
            .limitPrice(limitPrice)
            .build();
        log.info("Submitting OCO order: {}", request);
        
        try {
            final List<OrderResponse> responses = privateApi.closeOrder(request);
            final List<Long> orderIds = responses.stream()
                .map(OrderResponse::getOrderId)
                .collect(Collectors.toList());
            log.info("OCO order submitted successfully, order ids: {}", orderIds);
            return orderIds;
        } catch (Exception e) {
            log.error("Failed to submit OCO order", e);
            throw new OrderPlacementException("Failed to submit OCO order", e);
        }
    }
}
