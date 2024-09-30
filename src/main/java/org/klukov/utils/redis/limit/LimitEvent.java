package org.klukov.utils.redis.limit;

public interface LimitEvent {
    String getUserId();

    String getEventId();
}
