package com.takuro_tamura.autofx.infrastructure.datasource.mapper;

import com.takuro_tamura.autofx.TestcontainersConfiguration;
import com.takuro_tamura.autofx.infrastructure.datasource.entity.OrderDataModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
public class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testSelectByDateRange() {
        final var filledDataSql = "INSERT INTO `order` (order_id, currency_pair, side, size, status, created_datetime, fill_datetime, fill_price) " +
            "VALUES (1, 'USD_JPY', 'BUY', 10000, 'FILLED', '2024-01-01 10:00:00.000', '2024-01-01 10:00:00.000', 150.100), " +
            "(2, 'EUR_JPY', 'SELL', 10000, 'FILLED', '2024-01-14 10:00:00.000', '2024-01-01 10:00:00.000', 170.013), " +
            "(3, 'EUR_USD', 'SELL', 10000, 'FILLED', '2024-02-01 10:00:00.000', '2024-01-01 10:00:00.000', 1.2000)";
        jdbcTemplate.execute(filledDataSql);

        final var closeDataSql = "INSERT INTO `order` (order_id, currency_pair, side, size, status, created_datetime, close_datetime, close_price) " +
            "VALUES (4, 'USD_JPY', 'BUY', 10000, 'CLOSED', '2024-01-01 10:00:00.000', '2024-01-10 10:00:00.000', 151.500), " +
            "(5, 'EUR_JPY', 'SELL', 10000, 'CLOSED', '2024-01-14 10:00:00.000', '2024-01-20 10:00:00.000', 169.500), " +
            "(6, 'EUR_USD', 'SELL', 10000, 'CLOSED', '2024-02-01 10:00:00.000', '2024-02-10 10:00:00.000', 1.1800)";
        jdbcTemplate.execute(closeDataSql);

        final var startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        final var endDate = LocalDateTime.of(2024, 1, 31, 23, 59);
        final List<OrderDataModel> result = orderMapper.selectByDateRange(startDate, endDate);

        assertEquals(4, result.size());
        assertTrue(result.stream().anyMatch(order -> order.getCreatedDatetime().isAfter(startDate)));
        assertTrue(result.stream().anyMatch(order -> order.getCreatedDatetime().isBefore(endDate)));
    }
}
