package com.takuro_tamura.autofx.domain.model.entity;

import com.takuro_tamura.autofx.domain.model.value.ConfigValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ConfigParameter {
    private final String key;
    private ConfigValue value;
}
