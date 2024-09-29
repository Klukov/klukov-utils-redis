package org.klukov.utils.redis.limit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "klukov.utils.redis.limiter")
public record UserActionLimiterProperties(
        Duration limitDuration,
        Duration maxConfirmationDuration,
        String redisKeyPrefix
) {
    private static final String DEFAULT_PREFIX = "klukov-utils-redis-limiter";

    public UserActionLimiterProperties() {
        this(Duration.ofDays(1), Duration.ofMinutes(5), DEFAULT_PREFIX);
    }
}
