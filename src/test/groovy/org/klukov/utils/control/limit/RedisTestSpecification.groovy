package org.klukov.utils.control.limit

import org.testcontainers.containers.GenericContainer
import spock.lang.Specification

class RedisTestSpecification extends Specification {

    static protected GenericContainer redis = new GenericContainer<>("redis:7.4")
            .withExposedPorts(6379)

    def setupSpec() {
        redis.start()
    }

    def cleanupSpec() {
        redis.stop()
    }
}
