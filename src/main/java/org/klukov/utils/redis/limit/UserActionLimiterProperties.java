package org.klukov.utils.redis.limit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klukov.utils.redis.limiter")
public record UserActionLimiterProperties(int limitPeriodInHours, String redisKeyPrefix) {
    private static final String DEFAULT_PREFIX = "klukov-utils-redis-limiter";

    public UserActionLimiterProperties() {
        this(24, DEFAULT_PREFIX);
    }
}
