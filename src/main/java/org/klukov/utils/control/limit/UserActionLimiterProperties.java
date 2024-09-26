package org.klukov.utils.control.limit;

import java.time.Duration;
import lombok.Builder;

@Builder
public record UserActionLimiterProperties(
        Duration limitPeriod, String redisKeyPrefix, String redisHost, int redisPort) {}
