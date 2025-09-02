package com.takuro_tamura.autofx.infrastructure.datasource.mapper;

import com.takuro_tamura.autofx.infrastructure.datasource.entity.ConfigParameterDataModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConfigParameterMapper {
    @Select(
        "SELECT " +
        "   `key`, " +
        "   value " +
        "FROM " +
        "   config_parameter " +
        "WHERE" +
        "   `key` = #{key}"
    )
    ConfigParameterDataModel selectByKey(String key);

    @Update(
        "UPDATE " +
        "   config_parameter " +
        "SET " +
        "   value = #{value} " +
        "WHERE " +
        "   `key` = #{key}"
    )
    int update(ConfigParameterDataModel entity);
}
