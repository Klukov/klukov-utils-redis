package org.klukov.utils.redis

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class RedisTestSpecification extends Specification {

    @Shared
    static protected GenericContainer redis = new GenericContainer<>("redis:7.4")
            .withExposedPorts(6379)

    @DynamicPropertySource
    static void initRedis(DynamicPropertyRegistry registry) {
        redis.start()
        registry.add("spring.data.redis.host", () -> redis.getHost())
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString())
    }
}
