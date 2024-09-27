package org.klukov.utils.redis.limit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UserActionLimiterProperties.class)
class UserActionLimiterConfig {}
