package com.vaxly.schedulerservice;

import com.vaxly.schedulerservice.scheduler.RateRefreshScheduler;
import com.vaxly.vaxlyshared.constants.RedisKeys;
import com.vaxly.vaxlyshared.service.SqsProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RateRefreshSchedulerTest {

    @Mock
    ValueOperations<String, String> valueOperations;

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    SqsProducerService sqsProducerService;

    @Mock
    ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    RateRefreshScheduler rateRefreshScheduler;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(rateRefreshScheduler, "topPairsCount", 5L);
        ReflectionTestUtils.setField(rateRefreshScheduler, "refreshInterval", 60000L);

        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("should push top N pairs to SQS")
    public void testRefreshPopularRates_pushesEligiblePairsToSqs() {
        Set<String> mockTopPairs = new HashSet<>();
        mockTopPairs.add("BTCUSD");
        mockTopPairs.add("ETHUSD");

        when(zSetOperations.reverseRange(RedisKeys.USAGE_ZSET_KEY, 0, 4)).thenReturn(mockTopPairs);
        when(valueOperations.get(RedisKeys.lastRefreshKey("BTCUSD"))).thenReturn(null);
        when(valueOperations.get(RedisKeys.lastRefreshKey("ETHUSD"))).thenReturn(null);
        when(valueOperations.get(RedisKeys.inflightKey("BTCUSD"))).thenReturn(null);
        when(valueOperations.get(RedisKeys.inflightKey("ETHUSD"))).thenReturn(null);

        rateRefreshScheduler.refreshPopularRates();

        verify(sqsProducerService, times(1)).sendMessage("BTCUSD");
        verify(sqsProducerService, times(1)).sendMessage("ETHUSD");
        verify(valueOperations, times(1))
                .set(eq(RedisKeys.inflightKey("BTCUSD")), eq("true"), anyLong(), eq(TimeUnit.SECONDS));
        verify(valueOperations, times(1))
                .set(eq(RedisKeys.inflightKey("ETHUSD")), eq("true"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("should not process pairs that have been recently refreshed")
    public void testRefreshPopularRates_ignoresRecentlyRefreshedPairs() {
        Set<String> mockTopPairs = new HashSet<>();
        mockTopPairs.add("BTCUSD");

        when(zSetOperations.reverseRange(RedisKeys.USAGE_ZSET_KEY, 0, 4)).thenReturn(mockTopPairs);
        long recentTime = System.currentTimeMillis() - 1000; // refreshed 1s ago
        when(valueOperations.get(RedisKeys.lastRefreshKey("BTCUSD"))).thenReturn(String.valueOf(recentTime));

        rateRefreshScheduler.refreshPopularRates();

        verify(sqsProducerService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("should not process pairs that are already in-flight")
    public void testRefreshPopularRates_ignoresInFlightPairs() {
        Set<String> mockTopPairs = new HashSet<>();
        mockTopPairs.add("ETHUSD");

        when(zSetOperations.reverseRange(RedisKeys.USAGE_ZSET_KEY, 0, 4)).thenReturn(mockTopPairs);
        when(valueOperations.get(RedisKeys.lastRefreshKey("ETHUSD"))).thenReturn(null);
        when(valueOperations.get(RedisKeys.inflightKey("ETHUSD"))).thenReturn("true");

        rateRefreshScheduler.refreshPopularRates();

        verify(sqsProducerService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("should handle null top pairs from Redis gracefully")
    public void testRefreshPopularRates_handlesNullPopularPairs() {
        when(zSetOperations.reverseRange(RedisKeys.USAGE_ZSET_KEY, 0, 4)).thenReturn(null);

        rateRefreshScheduler.refreshPopularRates();

        verifyNoInteractions(sqsProducerService);
    }

    @Test
    @DisplayName("should handle empty top pairs from Redis gracefully")
    public void testRefreshPopularRates_handlesEmptyPopularPairs() {
        when(zSetOperations.reverseRange(RedisKeys.USAGE_ZSET_KEY, 0, 4)).thenReturn(Collections.emptySet());

        rateRefreshScheduler.refreshPopularRates();

        verifyNoInteractions(sqsProducerService);
    }
}
