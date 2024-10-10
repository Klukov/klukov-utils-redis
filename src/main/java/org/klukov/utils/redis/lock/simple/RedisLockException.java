package org.klukov.utils.redis.lock.simple;

public class RedisLockException extends RuntimeException {

    public RedisLockException(final String message) {
        super(message);
    }
}
