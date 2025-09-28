package com.vaxly.conversionservice.service;

import com.vaxly.vaxlyshared.constants.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to manage usage counters in Redis using a Sorted Set.
 * This class tracks the usage frequency of currency pairs, enabling retrieval
 * of the most popular pairs.
 */
@Service
public class UsageCounterService {

    private final StringRedisTemplate redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(UsageCounterService.class);
    // Key for the sorted set storing usage counts

    public UsageCounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically increments the usage count for a given currency pair in the sorted set.
     *
     * @param currencyPair The currency pair to increment (e.g., "USD_EUR").
     */
    public void incrementUsage(String currencyPair) {
        redisTemplate.opsForZSet().incrementScore(RedisKeys.USAGE_ZSET_KEY, currencyPair, 1);
        logger.info("Incremented usage for currency pair '{}'.", currencyPair);
    }
}