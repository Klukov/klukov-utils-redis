package org.klukov.utils.redis.lock

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
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
    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(1)

    @Autowired
    RedisTemplate<String, String> template

    @Autowired
    SimpleRedisLock sub

    @Autowired
    SimpleRedisLockProperties properties

    def cleanup() {
        template.keys('*').each {
            template.opsForValue().getAndDelete(it)
        }
    }

    def "should acquire new lock and lock should be with proper prefix"() {
        when:
        def acquireFirstKey = sub.acquire(KEY1, DEFAULT_DURATION)
        def value = template.opsForValue().get(properties.redisPrefix() + KEY1)
        print("REDIS PREFIX: " + properties.redisPrefix())

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
                .atMost(2, TimeUnit.SECONDS)
                .pollDelay(800, TimeUnit.MILLISECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .until(() -> sub.acquire(KEY2, DEFAULT_DURATION))
        def acquireFirstKeyAfterRelease = sub.acquire(KEY1, DEFAULT_DURATION)

        then:
        acquireFirstKey
        acquireSecondKey
        !acquireFirstKeySecondTime
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
        !acquireFirstKeySecondTime
        releaseFirstKey
        acquireFirstKeyAfterRelease

        cleanup:
        template.opsForValue().getAndDelete(properties.redisPrefix() + KEY1)
    }

    def "should release return false if lock does not exist"() {
        when:
        def release = sub.release(KEY1)

        then:
        !release
    }

    def "should release return false if lock does not exist after release"() {
        when:
        def acquire = sub.acquire(KEY1, DEFAULT_DURATION)
        def release = sub.release(KEY1)
        def secondRelease = sub.release(KEY1)

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
        (1..numberOfThreads).each {
            executor.submit {
                beforeProcessingBarrier.await()
                if (sub.acquire(KEY1, DEFAULT_DURATION)) {
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
