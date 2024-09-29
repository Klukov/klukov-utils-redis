package org.klukov.utils.redis.limit;

import org.springframework.stereotype.Component;

@Component
class DefaultUserActionLimiterTimeProvider implements UserActionLimiterTimeProvider {

    @Override
    public long getCurrentEpoch() {
        return System.currentTimeMillis();
    }
}
