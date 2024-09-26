package org.klukov.utils.control.limit;

import java.time.Instant;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class LimitEventDto implements LimitEvent {
    @NonNull String userId;
    @NonNull Instant timestamp;
}
