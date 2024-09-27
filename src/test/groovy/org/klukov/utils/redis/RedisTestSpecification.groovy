package org.klukov.utils.redis

import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Specification

@Testcontainers
class RedisTestSpecification extends Specification {

    static protected GenericContainer redis = new GenericContainer<>("redis:7.4")
            .withExposedPorts(6379)

    def setupSpec() {
        redis.start()
        System.setProperty("spring.data.redis.host", redis.getHost())
        System.setProperty("spring.data.redis.port", redis.getMappedPort(6379).toString())
    }

    def cleanupSpec() {
        redis.stop()
    }
}
