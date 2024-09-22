package org.klukov.utils.control;

import java.time.Instant;

public interface LimitEvent {
    String getUserId();
    Instant getTimestamp();
}
