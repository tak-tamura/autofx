package com.takuro_tamura.autofx.domain.model.entity;

import java.util.Optional;

public interface ConfigParameterRepository {
    Optional<ConfigParameter> findByKey(String key);

    int update(ConfigParameter configParameter);
}
