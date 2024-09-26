package org.klukov.utils.control.limit;

import java.time.Instant;

public interface LimitEvent {
    String getUserId();
    Instant getTimestamp();
}
