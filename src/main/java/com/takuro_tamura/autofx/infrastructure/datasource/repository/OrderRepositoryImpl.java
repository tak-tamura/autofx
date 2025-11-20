package com.takuro_tamura.autofx.infrastructure.datasource.repository;

import com.takuro_tamura.autofx.domain.model.entity.Order;
import com.takuro_tamura.autofx.domain.model.entity.OrderRepository;
import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.infrastructure.datasource.entity.OrderDataModel;
import com.takuro_tamura.autofx.infrastructure.datasource.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderMapper orderMapper;

    @Override
    public Optional<Order> findByOrderId(long orderId) {
        final OrderDataModel entity = orderMapper.selectByOrderId(orderId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(entity.toModel());
    }

    @Override
    public Optional<Order> findLatestByCurrencyPairWithLock(CurrencyPair currencyPair) {
        final OrderDataModel entity = orderMapper.selectLatestByCurrencyPairForUpdate(currencyPair);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(entity.toModel());
    }

    @Override
    public List<Order> findByCurrencyPairAfterTime(CurrencyPair currencyPair, LocalDateTime time) {
        return orderMapper.selectByCurrencyPairAfterTime(currencyPair, time)
            .stream()
            .map(OrderDataModel::toModel)
            .toList();
    }

    @Override
    public List<Order> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return orderMapper.selectByDateRange(start, end)
            .stream()
            .map(OrderDataModel::toModel)
            .toList();
    }

    @Override
    public void save(Order order) {
        final OrderDataModel entity = new OrderDataModel(order);
        orderMapper.insert(entity);
    }

    @Override
    public void update(Order order) {
        final OrderDataModel entity = new OrderDataModel(order);
        orderMapper.update(entity);
    }
}
