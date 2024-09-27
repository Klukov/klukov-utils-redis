package org.klukov.utils.redis.limit

import java.time.Instant
import org.klukov.utils.redis.KlukovUtilsRedisTestApp
import org.klukov.utils.redis.RedisTestSpecification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.spock.Testcontainers
import spock.lang.Ignore

@Testcontainers
@SpringBootTest(classes = KlukovUtilsRedisTestApp.class)
@ActiveProfiles("test")
class UserActionLimiterTest extends RedisTestSpecification {

    @Autowired
    UserActionLimiter sub

    @Ignore
    def "should allow actions up to the limit"() {
        given:
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

    @Ignore
    def "should allow more actions if limit increase"() {
        given:
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
}
