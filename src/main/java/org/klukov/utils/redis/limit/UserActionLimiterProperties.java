package org.klukov.utils.redis.limit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klukov.utils.redis.limiter")
record UserActionLimiterProperties(
        Duration limitDuration, Duration maxConfirmationDuration, String redisKeyPrefix) {}
