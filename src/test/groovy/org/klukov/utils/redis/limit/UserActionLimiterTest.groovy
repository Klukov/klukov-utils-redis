package org.klukov.utils.redis.limit

import java.time.Instant
import org.klukov.utils.redis.RedisTestSpecification
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.spock.Testcontainers

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class UserActionLimiterTest extends RedisTestSpecification {

    private static long NOW = 1727691450001

    @Autowired
    UserActionLimiter sub

    @Autowired
    RedisTemplate<String, String> template

    @SpringBean
    UserActionLimiterTimeProvider timeProvider = Mock()

    def cleanup() {
        template.keys('*').each {
            template.opsForValue().getAndDelete(it)
        }
    }

    def "should allow actions up to the limit"() {
        when:
        mockTimeWithStepOfMillis()
        println("CURRENT TIME: " + System.currentTimeMillis())
        def result1 = sub.isActionAllowed(simpleUserAction(), 2)
        def result2 = sub.isActionAllowed(simpleUserAction(), 2)
        def result3 = sub.isActionAllowed(simpleUserAction(), 2)

        then:
        assertLimit(result1, result2, result3)
    }

    def "should deduplicate action if each action has the same id"() {
        given:
        mockTimeWithStepOfMillis()
        def action = simpleUserAction()

        when:
        def result1 = sub.isActionAllowed(action, 2)
        def result2 = sub.isActionAllowed(action, 2)
        def result3 = sub.isActionAllowed(action, 2)

        then:
        result1
        !result2
        !result3
    }

    def "should allow more actions if limit increase"() {
        given:
        mockTimeWithStepOfMillis()

        when:
        def result1 = sub.isActionAllowed(simpleUserAction(), 2)
        def result2 = sub.isActionAllowed(simpleUserAction(), 2)
        def result3 = sub.isActionAllowed(simpleUserAction(), 2)
        def result4 = sub.isActionAllowed(simpleUserAction(), 3)
        def result5 = sub.isActionAllowed(simpleUserAction(), 3)

        then:
        assertLimit(result1, result2, result3)
        result4
        !result5
    }

    def "should new actions be allowed after no confirmation"() {
        given:
        timeProvider.getCurrentEpoch() >>> [NOW + 1, NOW + 2, NOW + 3, NOW + 1001, NOW + 1002, NOW + 1003]

        when:
        def result1 = sub.isActionAllowed(simpleUserAction(), 2)
        def result2 = sub.isActionAllowed(simpleUserAction(), 2)
        def result3 = sub.isActionAllowed(simpleUserAction(), 2)
        assertLimit(result1, result2, result3)

        then:
        def result4 = sub.isActionAllowed(simpleUserAction(), 2)
        def result5 = sub.isActionAllowed(simpleUserAction(), 2)
        def result6 = sub.isActionAllowed(simpleUserAction(), 2)
        assertLimit(result4, result5, result6)
    }

    def "should action processed confirmation works"() {
        given:
        mockTimeWithStepOfMillis()
        def action1 = simpleUserAction()

        when:
        def actionAllowed = sub.isActionAllowed(action1, 2)
        assert actionAllowed

        then:
        def confirmation = sub.actionProcessed(action1)
        assert confirmation
    }

    def "should action processed confirmation return false when no action was done"() {
        given:
        mockTimeWithStepOfMillis()

        when:
        def confirmation = sub.actionProcessed(simpleUserAction())

        then:
        assert !confirmation
    }

    def "should confirm action, and after confirmation time is still should be valid"() {
        given:
        timeProvider.getCurrentEpoch() >>> [NOW + 1, NOW + 2, NOW + 3, NOW + 4, NOW + 5, NOW + 1001, NOW + 1002, NOW + 1003]
        def action1 = simpleUserAction()
        def action2 = simpleUserAction()

        when:
        def result1 = sub.isActionAllowed(action1, 2)
        def result2 = sub.isActionAllowed(action2, 2)
        def result3 = sub.isActionAllowed(simpleUserAction(), 2)
        assertLimit(result1, result2, result3)
        def confirmation1 = sub.actionProcessed(action1)
        def confirmation2 = sub.actionProcessed(action2)
        assertConfirmations(confirmation1, confirmation2)

        then:
        def result4 = sub.isActionAllowed(simpleUserAction(), 2)
        assert !result4
    }

    def "should new actions be allowed in new duration period"() {
        given:
        timeProvider.getCurrentEpoch() >>> [NOW + 1, NOW + 2, NOW + 3, NOW + 4, NOW + 5, NOW + 5001, NOW + 5002, NOW + 5003]

        when:
        def event1 = simpleUserAction()
        def event2 = simpleUserAction()
        def result1 = sub.isActionAllowed(event1, 2)
        def result2 = sub.isActionAllowed(event2, 2)
        def result3 = sub.isActionAllowed(simpleUserAction(), 2)
        assertLimit(result1, result2, result3)
        def confirmation1 = sub.actionProcessed(event1)
        def confirmation2 = sub.actionProcessed(event2)
        assertConfirmations(confirmation1, confirmation2)

        then:
        def result4 = sub.isActionAllowed(simpleUserAction(), 2)
        def result5 = sub.isActionAllowed(simpleUserAction(), 2)
        def result6 = sub.isActionAllowed(simpleUserAction(), 2)
        assertLimit(result4, result5, result6)
    }

    private void mockTimeWithStepOfMillis(int stepSize = 1) {
        def counter = 0
        timeProvider.getCurrentEpoch() >> { NOW + stepSize * counter++ }
    }

    private static void assertLimit(boolean result1, boolean result2, boolean result3) {
        assert result1
        assert result2
        assert !result3
    }

    private static void assertConfirmations(boolean confirmation1, boolean confirmation2) {
        assert confirmation1
        assert confirmation2
    }

    private static LimitEventDto simpleUserAction() {
        LimitEventDto.builder()
                .userId("user")
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build()
    }
}
