package org.klukov.utils.control.limit;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserActionLimiter {

    private final UserActionLimiterProperties properties;

    public boolean isActionAllowed(LimitEvent limitEvent, int currentLimit) {
        return false;
    }
}
