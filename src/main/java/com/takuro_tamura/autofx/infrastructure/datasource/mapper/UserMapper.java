package com.takuro_tamura.autofx.infrastructure.datasource.mapper;

import com.takuro_tamura.autofx.infrastructure.datasource.entity.UserDataModel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select(
        "SELECT" +
        "   id, " +
        "   username, " +
        "   password, " +
        "   roles " +
        "FROM " +
        "   users " +
        "WHERE " +
        "   username = #{username}"
    )
    UserDataModel selectByUsername(String username);

    @Insert(
        "INSERT INTO " +
        "   users(username, password, roles) " +
        "VALUES (" +
        "   #{username}, " +
        "   #{password}, " +
        "   #{roles}" +
        ")"
    )
    void insert(UserDataModel user);
}
