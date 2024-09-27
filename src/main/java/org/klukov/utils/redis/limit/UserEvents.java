package org.klukov.utils.redis.limit;

import java.time.Instant;
import java.util.Collection;

public interface UserEvents {
    String getUserId();

    Collection<Instant> getTimestamp();
}
