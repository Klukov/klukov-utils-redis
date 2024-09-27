package org.klukov.utils.redis.lock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SimpleRedisLockProperties.class)
class SimpleRedisLockConfig {}
