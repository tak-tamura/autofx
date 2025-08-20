package com.takuro_tamura.autofx.infrastructure.datasource.repository;

import com.takuro_tamura.autofx.domain.model.entity.ConfigParameter;
import com.takuro_tamura.autofx.domain.model.entity.ConfigParameterRepository;
import com.takuro_tamura.autofx.infrastructure.datasource.entity.ConfigParameterDataModel;
import com.takuro_tamura.autofx.infrastructure.datasource.mapper.ConfigParameterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConfigParameterRepositoryImpl implements ConfigParameterRepository {
    private final ConfigParameterMapper mapper;

    @Override
    public Optional<ConfigParameter> findByKey(String key) {
        final ConfigParameterDataModel model = mapper.selectByKey(key);
        if (model == null) {
            return Optional.empty();
        }
        return Optional.of(model.toModel());
    }

    @Override
    public void update(ConfigParameter configParameter) {
        final ConfigParameterDataModel model = new ConfigParameterDataModel(configParameter);
        mapper.update(model);
    }
}
