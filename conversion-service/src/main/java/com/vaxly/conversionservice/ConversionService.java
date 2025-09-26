package com.vaxly.conversionservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaxly.conversionservice.dtos.ConversionResponseDto;
import com.vaxly.conversionservice.dtos.RateInfoDto;
import com.vaxly.conversionservice.enums.StateFlag;
import com.vaxly.conversionservice.exceptions.DownStreamException;
import com.vaxly.conversionservice.security.AwsCognitoTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Optional;

@Service
public class ConversionService {

    private final RedisTemplate<String, RateInfoDto> redisTemplate;
    private final WebClient webClient;
    private final  AwsCognitoTokenProvider tokenProvider;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(ConversionService.class);

    public ConversionService(RedisTemplate<String, RateInfoDto> redisTemplate, WebClient webClient, AwsCognitoTokenProvider tokenProvider) {
        this.redisTemplate = redisTemplate;
        this.webClient = webClient;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Converts an amount from one currency to another using a tiered data source strategy.
     * The process prioritizes speed by checking a local cache first, before falling back
     * to a remote service if no data is found.
     * <p>
     * 1. **Cache First**: Attempts to retrieve the conversion rate from Redis.
     * 2. **API Fallback**: If the rate is not in the cache, it calls the historical-service API
     * to fetch the rate.
     * 3. **Unavailable**: If both sources fail, it returns a response with an 'UNAVAILABLE' state.
     *
     * @param from   Source currency code (e.g., "USD").
     * @param to     Target currency code (e.g., "EUR").
     * @param amount Amount to convert.
     * @return A {@link ConversionResponseDto} with the conversion result and data source state.
     */
    public ConversionResponseDto convert(String from, String to, double amount) {
        String currencyPairKey = from + "_" + to;
        logger.info("Starting conversion for {} from {} to {}. Checking cache for key: {}", amount, from, to, currencyPairKey);

        Optional<RateInfoDto> cachedData = getCachedRate(currencyPairKey);
        if (cachedData.isPresent()) {
            RateInfoDto data = cachedData.get();
            logger.info("Rate for {} found in cache. Source: {}", currencyPairKey, data.getSource());
            return new ConversionResponseDto(
                    from, to,
                    data.getRate(),
                    amount * data.getRate(),
                    data.getSource(),
                    data.getTimestamp(),
                    StateFlag.CACHED
            );
        }

        logger.info("Rate for {} not found in cache. Falling back to external API.", currencyPairKey);
        String accessToken = tokenProvider.getAccessToken();
        Optional<RateInfoDto> historicalData = getHistoricalRate(currencyPairKey, accessToken);
        if (historicalData.isPresent()) {
            RateInfoDto data = historicalData.get();
            logger.info("Successfully fetched rate for {} from external API. Source: {}", currencyPairKey, data.getSource());
            return new ConversionResponseDto(
                    from, to,
                    data.getRate(),
                    amount * data.getRate(),
                    data.getSource(),
                    data.getTimestamp(),
                    StateFlag.FALLBACK_DB
            );
        }

        logger.warn("Rate for {} not found in cache or external API. Returning UNAVAILABLE status.", currencyPairKey);

        // Publish a background refresh message for this currency pair to trigger async rate population
        // TODO: Publish a background refresh message for this currency pair to trigger async rate population
        return new ConversionResponseDto(from, to, 0.0, 0.0, null, null, StateFlag.UNAVAILABLE);
    }

    /**
     * Retrieves a currency conversion rate from the Redis cache.
     *
     * @param key The currency pair key (e.g., "USD_EUR").
     * @return An {@link Optional} containing the cached {@link RateInfoDto}, or empty if not found.
     */
    private Optional<RateInfoDto> getCachedRate(String key) {
        logger.debug("Attempting to retrieve rate from Redis with key: {}", key);
        Optional<RateInfoDto> result = Optional.ofNullable(redisTemplate.opsForValue().get(key));
        if(result.isPresent()) {
            logger.debug("Rate for {} found in cache.", key);
        } else {
            logger.debug("Rate for {} not found in cache.", key);
        }
        return result;
    }

    /**
     * Fetches the historical rate for a currency pair from an external service.
     * <p>
     * This method performs a blocking HTTP GET request, authenticating with an
     * AWS Cognito access token. It deserializes the JSON response into a
     * {@link RateInfoDto}.
     *
     * @param currencyPair The currency pair string (e.g., "USD_EUR").
     * @param accessToken  The AWS Cognito access token for authorization.
     * @return An {@link Optional} containing the {@link RateInfoDto} if the request is successful and valid,
     * or empty if the response is malformed.
     * @throws RuntimeException if the HTTP request fails (e.g., network error, 4xx/5xx status codes).
     */
    public Optional<RateInfoDto> getHistoricalRate(String currencyPair, String accessToken) {
        logger.info("Fetching historical rate from external API for pair: {}", currencyPair);
        try {
            String responseBody = webClient.get()
                    .uri(currencyPair)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .map(body -> {
                                        logger.error("Downstream service returned an error. Status: {}, Body: {}", resp.statusCode(), body);
                                        return new DownStreamException("Downstream error: " + body);
                                    }))
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = mapper.readTree(responseBody);

            if (node.has("rate") && node.has("source")) {
                double rate = node.get("rate").asDouble();
                String source = node.get("source").asText();
                Instant timestamp = Instant.now();
                logger.info("Successfully received rate for {} from downstream API. Rate: {}", currencyPair, rate);
                return Optional.of(new RateInfoDto(source, timestamp, rate));
            } else {
                logger.warn("Response from downstream API for {} did not contain expected fields.", currencyPair);
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new DownStreamException("Failed to retrieve historical rate for currency pair " + currencyPair);
        }
    }
}