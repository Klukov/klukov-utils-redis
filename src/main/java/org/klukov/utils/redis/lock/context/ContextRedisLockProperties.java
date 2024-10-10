package org.klukov.utils.redis.lock.context;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klukov.utils.redis.lock.context")
record ContextRedisLockProperties(String redisPrefix) {}
