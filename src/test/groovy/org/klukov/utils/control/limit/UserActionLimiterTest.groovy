package org.klukov.utils.control.limit

import org.testcontainers.spock.Testcontainers
import redis.clients.jedis.Jedis

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Testcontainers
class UserActionLimiterTest extends RedisTestSpecification {

    def "should call redis"() {
        given:
        def jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))

        when:
        // Perform a simple Redis set/get operation
        jedis.set("test-key", "Hello, Redis!")
        String result = jedis.get("test-key")

        then:
        // Check that the value matches
        result == "Hello, Redis!"

        cleanup:
        jedis.close() // Close Jedis connection after test
    }

    def "should allow actions up to the limit"() {
        given:
        def sub = getUserActionLimiter()
        def action = simpleUserAction()

        when:
        def result1 = sub.isActionAllowed(action, 2)
        def result2 = sub.isActionAllowed(action, 2)
        def result3 = sub.isActionAllowed(action, 2)

        then:
        result1
        result2
        !result3
    }

    def "should allow more actions if limit increase"() {
        given:
        def sub = getUserActionLimiter()
        def action = simpleUserAction()

        when:
        def result1 = sub.isActionAllowed(action, 2)
        def result2 = sub.isActionAllowed(action, 2)
        def result3 = sub.isActionAllowed(action, 2)
        def result4 = sub.isActionAllowed(action, 3)
        def result5 = sub.isActionAllowed(action, 3)

        then:
        result1
        result2
        !result3
        result4
        !result5
    }

    private static LimitEventDto simpleUserAction() {
        LimitEventDto.builder()
                .userId("user")
                .timestamp(Instant.now())
                .build()
    }

    private static UserActionLimiter getUserActionLimiter() {
        new UserActionLimiter(
                UserActionLimiterProperties.builder()
                        .limitPeriod(Duration.of(1, ChronoUnit.DAYS))
                        .redisKeyPrefix("TEST")
                        .redisHost(redis.getHost())
                        .redisPort(redis.getMappedPort(6379))
                        .build())
    }
}
