package com.takuro_tamura.autofx.infrastructure.datasource.entity;

import com.takuro_tamura.autofx.domain.model.entity.ConfigParameter;
import com.takuro_tamura.autofx.domain.model.value.ConfigValue;
import lombok.Data;

@Data
public class ConfigParameterDataModel {
    private String key;
    private String value;

    public ConfigParameterDataModel() {}

    public ConfigParameterDataModel(ConfigParameter configParameter) {
        this.key = configParameter.getKey();
        this.value = configParameter.getValue().value();
    }

    public ConfigParameter toModel() {
        return new ConfigParameter(this.key, new ConfigValue(this.value));
    }
}
