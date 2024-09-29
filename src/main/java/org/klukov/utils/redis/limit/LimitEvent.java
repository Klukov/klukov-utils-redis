package org.klukov.utils.redis.limit;

import java.time.Instant;

public interface LimitEvent {
    String getUserId();
    String getEventId();

    Instant getTimestamp();
}
