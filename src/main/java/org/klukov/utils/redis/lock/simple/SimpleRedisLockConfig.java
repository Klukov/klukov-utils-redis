package org.klukov.utils.redis.lock.simple;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SimpleRedisLockProperties.class)
class SimpleRedisLockConfig {}
