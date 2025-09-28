package com.vaxly.vaxlyshared.config;

import com.vaxly.vaxlyshared.dtos.RateInfoDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private <T> RedisTemplate<String, T> createRedisTemplate(RedisConnectionFactory cf, Class<T> clazz) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(clazz));
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(clazz));

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, RateInfoDto> rateRedisTemplate(RedisConnectionFactory cf) {
        return createRedisTemplate(cf, RateInfoDto.class);
    }
}