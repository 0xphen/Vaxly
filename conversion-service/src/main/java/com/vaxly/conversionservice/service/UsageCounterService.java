package com.vaxly.conversionservice.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to manage usage counters in Redis.
 * This class encapsulates the logic for interacting with Redis to increment
 * and track the number of times a specific currency pair is accessed.
 */
@Service
public class UsageCounterService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(UsageCounterService.class);
    private static final String USAGE_KEY_PREFIX = "usage:count:";

    public UsageCounterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically increments the usage count for a given currency pair.
     *
     * @param currencyPair The currency pair to increment (e.g., "USD_EUR").
     */
    public void incrementUsage(String currencyPair) {
        String key = USAGE_KEY_PREFIX + currencyPair;
        Long newCount = redisTemplate.opsForValue().increment(key);
        logger.info("Incremented usage for currency pair '{}'. New count: {}", currencyPair, newCount);
    }
}