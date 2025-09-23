package com.vaxly.conversionservice;

import com.vaxly.conversionservice.dtos.ConversionResponseDto;
import com.vaxly.conversionservice.dtos.RateInfoDto;
import com.vaxly.conversionservice.enums.StateFlag;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class ConversionService {

    private final RedisTemplate<String, RateInfoDto> redisTemplate;

    public ConversionService(RedisTemplate<String, RateInfoDto> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Converts a currency amount by checking cache, then falling back to historical data.
     * Publishes a background refresh job if a live rate is not found.
     *
     * @param from   the source currency
     * @param to     the target currency
     * @param amount the amount to convert
     * @return a ConversionResponseDto with the converted amount and data state
     */
    public ConversionResponseDto convert(String from, String to, double amount) {
        String currencyPairKey = from + "_" + to;

        // Try to get a rate from Redis cache
        Optional<RateInfoDto> cachedData = getCachedRate(currencyPairKey);
        if (cachedData.isPresent()) {
            RateInfoDto data = cachedData.get();
            return new ConversionResponseDto(from, to, data.getRate(), amount * data.getRate(), data.getSource(), data.getTimestamp(), StateFlag.CACHED);
        }

        // Fallback to the historical database
        Optional<RateInfoDto> historicalData = getHistoricalRate(currencyPairKey);
        if (historicalData.isPresent()) {
            RateInfoDto data = historicalData.get();
            // TODO:
           // sqsService.publishRefreshMessage(from, to);
            return new ConversionResponseDto(from, to, data.getRate(), amount * data.getRate(), data.getSource(), data.getTimestamp(), StateFlag.FALLBACK_DB);
        }

        // TODO:
        // Return unavailable and trigger a refresh if all sources fail
        // sqsService.publishRefreshMessage(from, to);
        return new ConversionResponseDto(from, to, 0.0, 0.0, null, null, StateFlag.UNAVAILABLE);
    }

    /**
     * Attempts to retrieve a rate from the Redis cache.
     *
     * @param key the currency pair key (e.g., "USD_EUR")
     * @return an Optional containing the cached data, or empty if not found
     */
    private Optional<RateInfoDto> getCachedRate(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    /**
     * Mocks a REST API call to a separate History Service to get historical rate data.
     * In a real implementation, this would use a RestTemplate or WebClient to make an HTTP call.
     *
     * @param currencyPairKey The key for the currency pair (e.g., "USD_EUR")
     * @return An Optional containing the historical rate, or an empty Optional if not found.
     */
    public Optional<RateInfoDto> getHistoricalRate(String currencyPairKey) {
        // This is a mock implementation.
        // In a real scenario, you'd use a RestTemplate or WebClient to call the History Service API.

        // Example of a successful call
//        if ("USD_EUR".equals(currencyPairKey)) {
//            RateInfoDto mockData = new RateInfoDto("source", Instant.now(), 1.12);
//            return Optional.of(mockData);
//        }

        // Example of a failed call (data not found)
        return Optional.empty();
    }
}