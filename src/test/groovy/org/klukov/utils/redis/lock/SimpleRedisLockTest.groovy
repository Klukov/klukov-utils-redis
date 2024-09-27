package org.klukov.utils.redis.lock

import java.time.Duration
import java.util.concurrent.TimeUnit
import org.klukov.utils.redis.RedisTestSpecification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.shaded.org.awaitility.Awaitility
import org.testcontainers.spock.Testcontainers

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SimpleRedisLockTest extends RedisTestSpecification {

    private static final String KEY1 = "KEY-1"
    private static final String KEY2 = "KEY-2"
    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(2)

    @Autowired
    RedisTemplate<String, String> template

    @Autowired
    SimpleRedisLock sub

    @Autowired
    SimpleRedisLockProperties properties

    def cleanup() {
        template.opsForValue().getAndDelete(properties.redisPrefix() + KEY1)
        template.opsForValue().getAndDelete(properties.redisPrefix() + KEY2)
    }

    def "should acquire new lock and lock should be with proper prefix"() {
        when:
        def acquireFirstKey = sub.acquire(KEY1, DEFAULT_DURATION)
        def value = template.opsForValue().get(properties.redisPrefix() + KEY1)

        then:
        acquireFirstKey
        sub.LOCK_VALUE == value
    }

    def "should properly set locks"() {
        when:
        def acquireFirstKey = sub.acquire(KEY1, DEFAULT_DURATION)
        def acquireSecondKey = sub.acquire(KEY2, DEFAULT_DURATION)
        def acquireFirstKeySecondTime = sub.acquire(KEY1, DEFAULT_DURATION)
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollDelay(1600, TimeUnit.MILLISECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> sub.acquire(KEY2, DEFAULT_DURATION))
        def acquireFirstKeyAfterRelease = sub.acquire(KEY1, DEFAULT_DURATION)

        then:
        acquireFirstKey
        acquireSecondKey
        ! acquireFirstKeySecondTime
        acquireFirstKeyAfterRelease
    }

    def "should acquire new lock just after lock release"() {
        when:
        def acquireFirstKey = sub.acquire(KEY1, DEFAULT_DURATION)
        def acquireFirstKeySecondTime = sub.acquire(KEY1, DEFAULT_DURATION)
        def releaseFirstKey = sub.release(KEY1)
        def acquireFirstKeyAfterRelease = sub.acquire(KEY1, DEFAULT_DURATION)

        then:
        acquireFirstKey
        ! acquireFirstKeySecondTime
        releaseFirstKey
        acquireFirstKeyAfterRelease

        cleanup:
        template.opsForValue().getAndDelete(properties.redisPrefix() + KEY1)
    }

    def "should release return false if lock does not exist"() {
        when:
        def release = sub.release(KEY1)

        then:
        ! release
    }

    def "should release return false if lock does not exist after release"() {
        when:
        def acquire = sub.acquire(KEY1, DEFAULT_DURATION)
        def release = sub.release(KEY1)
        def secondRelease = sub.release(KEY1)

        then:
        acquire
        release
        ! secondRelease
    }
}
