package org.klukov.utils.redis.limit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
@EnableConfigurationProperties(UserActionLimiterProperties.class)
class UserActionLimiterConfig {

    @Bean
    public RedisScript<Boolean> confirmationScript() {
        return RedisScript.of(
                new ClassPathResource("redis/user-limiter-confirm.lua"), Boolean.class);
    }

    @Bean
    public RedisScript<Boolean> pendingScript() {
        return RedisScript.of(
                (new ClassPathResource("redis/user-limiter-pending.lua")), Boolean.class);
    }

    @Bean
    @ConditionalOnMissingBean(UserActionLimiterTimeProvider.class)
    public UserActionLimiterTimeProvider defaultTimeProvider() {
        return new DefaultUserActionLimiterTimeProvider();
    }
}
