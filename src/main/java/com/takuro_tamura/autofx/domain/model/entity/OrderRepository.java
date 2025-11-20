package com.takuro_tamura.autofx.domain.model.entity;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findByOrderId(long orderId);

    Optional<Order> findLatestByCurrencyPairWithLock(CurrencyPair currencyPair);

    List<Order> findByCurrencyPairAfterTime(CurrencyPair currencyPair, LocalDateTime time);

    List<Order> findByDateRange(LocalDateTime start, LocalDateTime end);

    void save(Order order);

    void update(Order order);
}
