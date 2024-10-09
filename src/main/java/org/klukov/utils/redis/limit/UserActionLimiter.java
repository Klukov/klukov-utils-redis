package org.klukov.utils.redis.limit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActionLimiter {

    private final UserActionLimiterProperties properties;
    private final ReactiveRedisTemplate<String, String> template;
    private final UserActionLimiterTimeProvider timeProvider;
    private final RedisScript<Boolean> pendingScript;
    private final RedisScript<Boolean> confirmationScript;

    /**
     * @return true if number of confirmed operation and non-expired unconfirmed operation are less
     *     than current limit. Otherwise, return false. In the case when event-id is duplicated
     *     return false.
     */
    public boolean isActionAllowed(LimitEvent limitEvent, int currentLimit) {
        var currentTimestamp = timeProvider.getCurrentEpoch();
        var flooredTimestamp = floorTimestamp(currentTimestamp);
        var pendingKey = getPendingKey(flooredTimestamp, limitEvent.getUserId());
        var finishedKey = getFinishedKey(flooredTimestamp, limitEvent.getUserId());
        List<String> keys = List.of(finishedKey, pendingKey);
        List<String> args =
                List.of(
                        String.valueOf(currentLimit),
                        limitEvent.getEventId(),
                        String.valueOf(getEventExpirationTime(currentTimestamp)),
                        String.valueOf(currentTimestamp),
                        String.valueOf(getRedisKeysTtl()));
        log.debug("isActionAllowed request to redis: keys: {}, args: {})", keys, args);
        return Boolean.TRUE.equals(
                template.execute(pendingScript, keys, args).single(false).block());
    }

    private long getRedisKeysTtl() {
        return properties.limitDuration().toSeconds() * 2;
    }

    private long getEventExpirationTime(long currentTimestamp) {
        return currentTimestamp + properties.maxConfirmationDuration().toMillis();
    }

    private String getPendingKey(long flooredTimestamp, String userId) {
        return properties.redisKeyPrefix() + ":pending:" + flooredTimestamp + ":" + userId;
    }

    private String getFinishedKey(long flooredTimestamp, String userId) {
        return properties.redisKeyPrefix() + ":finished:" + flooredTimestamp + ":" + userId;
    }

    private long floorTimestamp(long timestamp) {
        var durationMillis = properties.limitDuration().toMillis();
        return (timestamp / durationMillis) * durationMillis;
    }

    /**
     * @return true if marking event as processed finished successfully, otherwise return false
     */
    public boolean actionProcessed(LimitEvent limitEvent) {
        var currentTimestamp = timeProvider.getCurrentEpoch();
        var flooredTimestamp = floorTimestamp(currentTimestamp);
        var pendingKey = getPendingKey(flooredTimestamp, limitEvent.getUserId());
        var finishedKey = getFinishedKey(flooredTimestamp, limitEvent.getUserId());
        List<String> keys = List.of(finishedKey, pendingKey);
        List<String> args = List.of(limitEvent.getEventId(), String.valueOf(getRedisKeysTtl()));
        log.debug("actionProcessed request to redis: keys: {}, args: {})", keys, args);
        return Boolean.TRUE.equals(
                template.execute(confirmationScript, keys, args).single(false).block());
    }
}
