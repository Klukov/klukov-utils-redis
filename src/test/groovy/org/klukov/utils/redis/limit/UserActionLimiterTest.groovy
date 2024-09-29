package org.klukov.utils.redis.limit

import org.springframework.data.redis.core.RedisTemplate
import spock.lang.Ignore

import java.time.Instant
import org.klukov.utils.redis.RedisTestSpecification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.spock.Testcontainers

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class UserActionLimiterTest extends RedisTestSpecification {

    @Autowired
    UserActionLimiter sub

    @Autowired
    RedisTemplate<String, String> template

    def cleanup() {
       template.keys('*').each {
            template.opsForValue().getAndDelete(it)
        }
    }

    @Ignore
    def "should allow actions up to the limit"() {
        when:
        def result1 = sub.isActionAllowed(simpleUserAction(), 2)
        def result2 = sub.isActionAllowed(simpleUserAction(), 2)
        def result3 = sub.isActionAllowed(simpleUserAction(), 2)

        then:
        printRedis()
        result1
        result2
        !result3
    }

    @Ignore
    def "should deduplicate action if each action has the same id"() {
        given:
        def action = simpleUserAction()

        when:
        def result1 = sub.isActionAllowed(action, 2)
        def result2 = sub.isActionAllowed(action, 2)
        def result3 = sub.isActionAllowed(action, 2)
        def result4 = sub.isActionAllowed(action, 2)

        then:
        printRedis()
        result1
        result2
        result3
        result4
    }

    @Ignore
    def "should allow more actions if limit increase"() {
        when:
        def result1 = sub.isActionAllowed(simpleUserAction(), 2)
        def result2 = sub.isActionAllowed(simpleUserAction(), 2)
        def result3 = sub.isActionAllowed(simpleUserAction(), 2)
        def result4 = sub.isActionAllowed(simpleUserAction(), 3)
        def result5 = sub.isActionAllowed(simpleUserAction(), 3)

        then:
        printRedis()
        result1
        result2
        !result3
        result4
        !result5
    }

    private printRedis() {
        template.keys('*').each {
            def value = template.opsForValue().get(it)
            println("KEY: $it : VALUE: $value")
        }
    }

    private static LimitEventDto simpleUserAction() {
        LimitEventDto.builder()
                .userId("user")
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build()
    }
}
