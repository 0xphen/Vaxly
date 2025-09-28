package com.vaxly.schedulerservice.scheduler;

import com.vaxly.vaxlyshared.service.SqsProducerService;
import com.vaxly.vaxlyshared.constants.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

/**
 * Scheduler for refreshing currency rates.
 *
 * - Fetches the most popular currency pairs from Redis.
 * - Filters pairs that are already in-flight or recently refreshed.
 * - Publishes eligible pairs to SQS for asynchronous processing by workers.
 *
 * Decouples scheduling logic from worker processing and avoids redundant API calls.
 */
@Component
public class RateRefreshScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RateRefreshScheduler.class);

    private final SqsProducerService sqsProducerService;
    private final StringRedisTemplate redisTemplate;

    @Value("${scheduler.topPairsCount}")
    private long topPairsCount;

    @Value("${scheduler.refreshInterval}")
    private long refreshInterval;

    public RateRefreshScheduler(SqsProducerService sqsProducerService, StringRedisTemplate redisTemplate) {
        this.sqsProducerService = sqsProducerService;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void logInjectedConfig() {
        logger.info("Scheduler configured to refresh the top {} popular pairs.", topPairsCount);
    }

    /**
     * Scheduled task that refreshes the top popular currency pairs.
     * Publishes each eligible pair to SQS while respecting in-flight and refresh intervals.
     */
    @Scheduled(fixedRateString = "${scheduler.fixedRate}")
    public void refreshPopularRates() {
        logger.info("Executing scheduled task to refresh popular currency rates.");
        // Get top N pairs from usage sorted set
        Set<String> topPairs = redisTemplate.opsForZSet()
                .reverseRange(RedisKeys.USAGE_ZSET_KEY, 0, topPairsCount - 1);

        if (topPairs == null || topPairs.isEmpty()) {
            logger.warn("No popular pairs found in Redis to refresh.");
            return;
        }

        // Filter pairs eligible for refresh (not in-flight & refresh interval exceeded)
        Set<String> eligiblePairs = getEligiblePairs(topPairs);
        if (eligiblePairs.isEmpty()) {
            logger.info("No pairs eligible for refresh at this time.");
            return;
        }

        logger.info("Publishing {} eligible pair(s) to SQS: {}", eligiblePairs.size(), eligiblePairs);

        // Mark each pair as in-flight and enqueue to SQS
        eligiblePairs.forEach(pair -> {
            String inflightKey = RedisKeys.inflightKey(pair);
            // Set in-flight with TTL to prevent duplicate processing
            redisTemplate.opsForValue().set(inflightKey, "true", 120, TimeUnit.SECONDS);
            sqsProducerService.sendMessage(pair);
        });
    }


    /**
     * Filters currency pairs to only those eligible for refresh:
     * - Not currently in-flight
     * - Last refresh exceeded the configured interval
     *
     * @param pairs set of candidate currency pairs
     * @return filtered set of pairs ready for refresh
     */
    private Set<String> getEligiblePairs(Set<String> pairs) {
        long now = System.currentTimeMillis();

        return pairs.stream()
                .filter(pair -> {
                    String lastRefreshStr = redisTemplate.opsForValue()
                            .get(RedisKeys.lastRefreshKey(pair));
                    if (lastRefreshStr == null) return true; // never refreshed
                    try {
                        long lastRefresh = Long.parseLong(lastRefreshStr);
                        return (now - lastRefresh) > refreshInterval;
                    } catch (NumberFormatException e) {
                        return true; // invalid value → consider eligible
                    }
                })
                .filter(pair -> redisTemplate.opsForValue()
                        .get(RedisKeys.inflightKey(pair)) == null)
                .collect(Collectors.toSet());
    }
}
