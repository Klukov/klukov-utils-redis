package org.klukov.utils.redis.lock.simple;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimpleRedisLock {
    private static final String LOCK_VALUE = "lock";

    private final SimpleRedisLockProperties properties;
    private final RedisTemplate<String, String> template;

    /**
     * @return true if lock acquired, false if lock is already acquired by other thread
     * @throws RedisLockException in case when redisTemplate return null value, which means that
     *     Command was used in transaction or pipeline
     */
    public boolean acquire(String name, Duration timeout) {
        var result = template.opsForValue().setIfAbsent(getKey(name), LOCK_VALUE, timeout);
        if (result == null) {
            throw new RedisLockException(
                    "Redis unexpectedly returned null. Command was used in transaction or pipeline");
        }
        return result;
    }

    /**
     * @return true if success, false if key not exist
     */
    public boolean release(String name) {
        var result = template.opsForValue().getAndDelete(getKey(name));
        return LOCK_VALUE.equals(result);
    }

    private String getKey(String name) {
        return properties.redisPrefix() + name;
    }
}
