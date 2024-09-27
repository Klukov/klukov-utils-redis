package org.klukov.utils.redis.limit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActionLimiter {

    private final UserActionLimiterProperties userActionLimiterProperties;
    private final RedisTemplate<String, String> template;

    public boolean isActionAllowed(LimitEvent limitEvent, int currentLimit) {
        return false;
    }
}
