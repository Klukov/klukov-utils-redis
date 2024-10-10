package org.klukov.utils.redis.lock.context;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.klukov.utils.redis.lock.RedisLockException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContextRedisLock {
    private final ContextRedisLockProperties properties;
    private final ReactiveStringRedisTemplate template;
    private final RedisScript<Boolean> acquireContextLockRedisScript;
    private final RedisScript<Boolean> releaseContextLockRedisScript;

    /**
     * @return true if lock acquired, false if lock is already acquired by other thread
     * @throws RedisLockException in case when redisTemplate return null value, which means that
     *     Command was used in transaction or pipeline
     */
    public boolean acquire(String name, String context, Duration timeout) {
        var keys = List.of(getKey(name));
        var args = List.of(context, String.valueOf(timeout.toSeconds()));
        return Boolean.TRUE.equals(
                template.execute(acquireContextLockRedisScript, keys, args).single(false).block());
    }

    /**
     * @return true if success, false if key not exist or value has different context
     */
    public boolean release(String name, String context) {
        var keys = List.of(getKey(name));
        var args = List.of(context);
        return Boolean.TRUE.equals(
                template.execute(releaseContextLockRedisScript, keys, args).single(false).block());
    }

    private String getKey(String name) {
        return properties.redisPrefix() + ":" + name;
    }
}
