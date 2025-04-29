package com.takuro_tamura.autofx.infrastructure.datasource.mapper;

import com.takuro_tamura.autofx.domain.model.value.CurrencyPair;
import com.takuro_tamura.autofx.infrastructure.datasource.entity.OrderDataModel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    @Select(
        "SELECT" +
        "   order_id," +
        "   currency_pair," +
        "   side," +
        "   size," +
        "   status," +
        "   created_datetime," +
        "   fill_datetime," +
        "   fill_price," +
        "   close_datetime," +
        "   close_price " +
        "FROM" +
        "   `order` " +
        "WHERE" +
        "   order_id = #{orderId} " +
        "FOR UPDATE"
    )
    OrderDataModel selectByOrderId(long orderId);

    @Select(
        "SELECT " +
        "   order_id," +
        "   currency_pair," +
        "   side," +
        "   size," +
        "   status," +
        "   created_datetime," +
        "   fill_datetime," +
        "   fill_price," +
        "   close_datetime," +
        "   close_price " +
        "FROM " +
        "   `order` " +
        "WHERE " +
        "   currency_pair = #{currencyPair} " +
        "ORDER BY created_datetime DESC " +
        "LIMIT 1 " +
        "FOR UPDATE"
    )
    OrderDataModel selectLatestByCurrencyPairForUpdate(CurrencyPair currencyPair);

    @Select(
        "SELECT" +
        "   order_id," +
        "   currency_pair," +
        "   side," +
        "   size," +
        "   status," +
        "   created_datetime," +
        "   fill_datetime," +
        "   fill_price," +
        "   close_datetime," +
        "   close_price " +
        "FROM (" +
        "   SELECT " +
        "       order_id," +
        "       currency_pair," +
        "       side, " +
        "       size, " +
        "       status, " +
        "       created_datetime, " +
        "       fill_datetime, " +
        "       fill_price, " +
        "       close_datetime, " +
        "       close_price " +
        "   FROM " +
        "       `order` " +
        "   WHERE " +
        "       currency_pair = #{currencyPair} " +
        "       AND created_datetime >= #{time} " +
        "       AND status IN ('FILLED', 'CLOSED') " +
        "   ORDER BY created_datetime DESC " +
        ") AS subquery " +
        "ORDER BY created_datetime ASC"
    )
    List<OrderDataModel> selectByCurrencyPairAfterTime(CurrencyPair currencyPair, LocalDateTime time);

    @Insert(
        "INSERT INTO `order` (" +
        "   order_id," +
        "   currency_pair," +
        "   side," +
        "   size," +
        "   status," +
        "   fill_datetime," +
        "   fill_price" +
        ") VALUES (" +
        "   #{orderId}," +
        "   #{currencyPair}," +
        "   #{side}," +
        "   #{size}," +
        "   #{status}," +
        "   #{fillDatetime}," +
        "   #{fillPrice}" +
        ")"
    )
    void insert(OrderDataModel order);

    @Update(
        "UPDATE " +
        "   `order` " +
        "SET " +
        "   status = #{status}, " +
        "   close_datetime = #{closeDatetime}," +
        "   close_price = #{closePrice}" +
        "WHERE " +
        "   order_id = #{orderId}"
    )
    void update(OrderDataModel order);
}
