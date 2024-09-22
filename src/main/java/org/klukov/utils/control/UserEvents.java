package org.klukov.utils.control;

import java.time.Instant;
import java.util.Collection;

public interface UserEvents {
    String getUserId();
    Collection<Instant> getTimestamp();
}
