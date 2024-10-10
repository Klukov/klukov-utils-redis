package org.klukov.utils.redis.lock.context

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.klukov.utils.redis.RedisTestSpecification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.shaded.org.awaitility.Awaitility
import org.testcontainers.spock.Testcontainers

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ContextRedisLockTest extends RedisTestSpecification {

    private static final String KEY1 = "KEY-1"
    private static final String KEY2 = "KEY-2"
    private static final String CONTEXT_1 = "CONTEXT-1"
    private static final String CONTEXT_2 = "CONTEXT-2"
    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(1)

    @Autowired
    StringRedisTemplate template

    @Autowired
    ContextRedisLock sub

    @Autowired
    ContextRedisLockProperties properties

    def cleanup() {
        template.keys('*').each {
            template.opsForValue().getAndDelete(it)
        }
    }

    def "should acquire new lock and lock should be with proper prefix"() {
        when:
        def acquireFirstKey = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)
        def value = template.opsForValue().get(properties.redisPrefix() + ":" + KEY1)

        then:
        acquireFirstKey
        value == CONTEXT_1
    }

    def "should two keys be able to add the same context"() {
        when:
        def acquireFirstKey = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)
        def acquireSecondKey = sub.acquire(KEY2, CONTEXT_1, DEFAULT_DURATION)

        then:
        acquireFirstKey
        acquireSecondKey
    }

    def "should one key be not able to lock with different context"() {
        when:
        def acquireFirst = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)
        def acquireSecond = sub.acquire(KEY1, CONTEXT_2, DEFAULT_DURATION)

        then:
        acquireFirst
        !acquireSecond
    }

    def "should acquire lock return true if the same context is passed"() {
        when:
        def acquireFirst = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)
        def acquireDifferentContext = sub.acquire(KEY1, CONTEXT_2, DEFAULT_DURATION)
        def acquireSecond = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)

        then:
        acquireFirst
        !acquireDifferentContext
        acquireSecond
    }

    def "should properly set locks"() {
        when:
        def acquireFirstKey = sub.acquire(KEY1, CONTEXT_2, DEFAULT_DURATION)
        def acquireSecondKey = sub.acquire(KEY2, CONTEXT_2, DEFAULT_DURATION)
        def acquireFirstKeyDifferentContext = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)
        Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .pollDelay(800, TimeUnit.MILLISECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .until(() -> sub.acquire(KEY2, CONTEXT_1, DEFAULT_DURATION))
        def acquireFirstKeyAfterRelease = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)
        def lockValueKey1 = template.opsForValue().get(properties.redisPrefix() + ":" + KEY1)

        then:
        acquireFirstKey
        acquireSecondKey
        !acquireFirstKeyDifferentContext
        acquireFirstKeyAfterRelease
        lockValueKey1 == CONTEXT_1
    }

    def "should acquire new lock just after lock release"() {
        when:
        def acquireFirstKey = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)
        def acquireFirstKeySecondTime = sub.acquire(KEY1, CONTEXT_2, DEFAULT_DURATION)
        def releaseFirstKey = sub.release(KEY1, CONTEXT_1)
        def acquireFirstKeyAfterRelease = sub.acquire(KEY1, CONTEXT_2, DEFAULT_DURATION)
        def lockValueKey1 = template.opsForValue().get(properties.redisPrefix() + ":" + KEY1)

        then:
        acquireFirstKey
        !acquireFirstKeySecondTime
        releaseFirstKey
        acquireFirstKeyAfterRelease
        lockValueKey1 == CONTEXT_2
    }

    def "should release return false if lock does not exist"() {
        when:
        def release = sub.release(KEY1, CONTEXT_1)

        then:
        !release
    }

    def "should release return false if lock has different context"() {
        when:
        def acquire = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)
        def release = sub.release(KEY1, CONTEXT_2)

        then:
        acquire
        !release
    }

    def "should release return false if lock does not exist after release"() {
        when:
        def acquire = sub.acquire(KEY1, CONTEXT_1, DEFAULT_DURATION)
        def release = sub.release(KEY1, CONTEXT_1)
        def secondRelease = sub.release(KEY1, CONTEXT_1)

        then:
        acquire
        release
        !secondRelease
    }

    def "should only one thread acquire a lock in multithreaded environment"() {
        given:
        int numberOfThreads = 32
        def beforeProcessingBarrier = new CyclicBarrier(numberOfThreads + 1)
        def afterProcessingLatch = new CountDownLatch(numberOfThreads)
        def executor = Executors.newFixedThreadPool(numberOfThreads)
        def lockAcquiredCount = new AtomicInteger(0)

        when:
        (1..numberOfThreads).each { index ->
            executor.submit {
                beforeProcessingBarrier.await()
                def context = "CONTEXT-" + String.valueOf(index)
                if (sub.acquire(KEY1, context, DEFAULT_DURATION)) {
                    lockAcquiredCount.incrementAndGet()
                }
                afterProcessingLatch.countDown()
            }
        }
        beforeProcessingBarrier.await()
        afterProcessingLatch.await()

        then:
        lockAcquiredCount.get() == 1
    }
}
