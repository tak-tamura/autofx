package com.takuro_tamura.autofx.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void save(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void save(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) redisTemplate.opsForValue().get(key));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void deleteKeysByValue(Object targetValue) {
        final ScanOptions options = ScanOptions.scanOptions().match("*").count(1000).build();
        final Set<String> keysToDelete = new HashSet<>();

        try (var cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                final String key = cursor.next();
                final Object value = redisTemplate.opsForValue().get(key);

                if (targetValue.equals(value)) {
                    keysToDelete.add(key);
                }
            }
        }

        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }
}
