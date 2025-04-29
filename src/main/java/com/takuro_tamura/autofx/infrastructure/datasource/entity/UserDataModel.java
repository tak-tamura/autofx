package com.takuro_tamura.autofx.infrastructure.datasource.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class UserDataModel {
    private Long id;

    private String username;

    private String password;

    private String roles;

    public UserDataModel(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.roles = authorities
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
    }
}
