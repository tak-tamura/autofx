package com.takuro_tamura.autofx.infrastructure.datasource.repository;

import com.takuro_tamura.autofx.domain.model.entity.User;
import com.takuro_tamura.autofx.domain.model.entity.UserRepository;
import com.takuro_tamura.autofx.infrastructure.datasource.entity.UserDataModel;
import com.takuro_tamura.autofx.infrastructure.datasource.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository  {
    private final UserMapper mapper;

    @Override
    public Optional<User> findByUsername(String username) {
        final UserDataModel user = mapper.selectByUsername(username);
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(new User(user.getUsername(), user.getPassword(), user.getRoles()));
    }

    @Override
    public void save(User user) {
        final UserDataModel entity = new UserDataModel(
            user.getUsername(),
            user.getPassword(),
            user.getAuthorities()
        );
        mapper.insert(entity);
    }
}
