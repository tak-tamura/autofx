package com.takuro_tamura.autofx.domain.service.config;

import com.takuro_tamura.autofx.domain.model.entity.ConfigParameterRepository;
import com.takuro_tamura.autofx.domain.model.value.ConfigValue;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigParameterService {
    private final ConfigParameterRepository configParameterRepository;

    @Cacheable(value = "configParameters", key = "#key")
    public Integer getInt(String key, Integer defaultValue) {
        return configParameterRepository.findByKey(key)
                .map(configParameter -> configParameter.getValue().valueAsInt())
                .orElse(defaultValue);
    }

    @Cacheable(value = "configParameters", key = "#key")
    public String getString(String key, String defaultValue) {
        return configParameterRepository.findByKey(key)
                .map(configParameter -> configParameter.getValue().value())
                .orElse(defaultValue);
    }

    @Cacheable(value = "configParameters", key = "#key")
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return configParameterRepository.findByKey(key)
                .map(configParameter -> configParameter.getValue().valueAsBoolean())
                .orElse(defaultValue);
    }

    @Cacheable(value = "configParameters", key = "#key")
    public Double getDouble(String key, Double defaultValue) {
        return configParameterRepository.findByKey(key)
                .map(configParameter -> configParameter.getValue().valueAsDouble())
                .orElse(defaultValue);
    }

    @CacheEvict(value = "configParameters", key = "#key")
    public boolean updateConfigParameter(String key, String value) {
        return configParameterRepository.findByKey(key)
                .map(configParameter -> {
                    configParameter.setValue(new ConfigValue(value));
                    return configParameterRepository.update(configParameter) > 0;
                })
                .orElse(false);
    }
}
