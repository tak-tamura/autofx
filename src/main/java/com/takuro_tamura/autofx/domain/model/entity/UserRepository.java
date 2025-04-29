package com.takuro_tamura.autofx.domain.model.entity;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);

    void save(User user);
}
