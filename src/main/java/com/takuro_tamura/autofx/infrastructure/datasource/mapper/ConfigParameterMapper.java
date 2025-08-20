package com.takuro_tamura.autofx.infrastructure.datasource.mapper;

import com.takuro_tamura.autofx.infrastructure.datasource.entity.ConfigParameterModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConfigParameterMapper {
    @Select(
        "SELECT " +
        "   key, " +
        "   value " +
        "FROM " +
        "   config_parameter " +
        "WHERE" +
        "   key = #{key}"
    )
    ConfigParameterModel selectByKey(String key);

    @Update(
        "UPDATE " +
        "   config_parameter " +
        "SET " +
        "   value = #{value} " +
        "WHERE " +
        "   key = #{key}"
    )
    void update(ConfigParameterModel entity);
}
