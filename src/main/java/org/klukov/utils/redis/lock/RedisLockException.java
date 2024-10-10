package org.klukov.utils.redis.lock;

public class RedisLockException extends RuntimeException {

    public RedisLockException(final String message) {
        super(message);
    }
}
