package org.klukov.utils.redis.limit;

public interface UserActionLimiterTimeProvider {
    long getCurrentEpoch();
}
