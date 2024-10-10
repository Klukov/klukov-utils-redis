package org.klukov.utils.redis.lock.context;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
@EnableConfigurationProperties(ContextRedisLockProperties.class)
class ContextRedisLockConfig {

    @Bean
    public RedisScript<Boolean> acquireContextLockRedisScript() {
        return RedisScript.of(
                (new ClassPathResource("redis/context-lock-acquire.lua")), Boolean.class);
    }

    @Bean
    public RedisScript<Boolean> releaseContextLockRedisScript() {
        return RedisScript.of(
                (new ClassPathResource("redis/context-lock-release.lua")), Boolean.class);
    }
}
