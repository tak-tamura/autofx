package com.takuro_tamura.autofx.infrastructure.datasource.mapper;

import com.takuro_tamura.autofx.infrastructure.datasource.entity.CandleDataModel;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CandleMapper {
    @SelectProvider(type = CandleSqlProvider.class, method = "findByTime")
    CandleDataModel findByTime(String table, LocalDateTime time);

    @Select({
        "SELECT ",
        "   id, time, currency_pair, open, close, high, low ",
        "FROM ",
        "   ( ",
        "       SELECT ",
        "           id, time, currency_pair, open, close, high, low ",
        "       FROM ",
        "           ${table} ",
        "       ORDER BY ",
        "           time DESC ",
        "       LIMIT ",
        "           #{limit} ",
        "   ) t ",
        "ORDER BY ",
        "   time ASC"
    })
    List<CandleDataModel> findAllWithLimit(String table, int limit);

    @Insert({
        "INSERT INTO ${table}(time, currency_pair, open, close, high, low) ",
        "VALUES(#{candle.time}, #{candle.currencyPair}, #{candle.open}, #{candle.close}, #{candle.high}, #{candle.low})"
    })
    int insert(String table, CandleDataModel candle);

    @Update({
        "UPDATE ${table} ",
        "SET close = #{candle.close}, high = #{candle.high}, low = #{candle.low} ",
        "WHERE id = #{candle.id}"
    })
    int update(String table, CandleDataModel candle);

    @Delete("TRUNCATE TABLE ${table}")
    void truncate(String table);

    class CandleSqlProvider {
        public String findByTime(@Param("table") String table, LocalDateTime time) {
            return new SQL() {
                {
                    SELECT("*");
                    FROM(table);
                    WHERE("time = #{time}");
                }
            }.toString();
        }
    }

}
