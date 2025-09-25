package com.vaxly.conversionservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaxly.conversionservice.dtos.ConversionResponseDto;
import com.vaxly.conversionservice.dtos.RateInfoDto;
import com.vaxly.conversionservice.enums.StateFlag;
import com.vaxly.conversionservice.security.AwsCognitoTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Optional;

@Service
public class ConversionService {

    private final RedisTemplate<String, RateInfoDto> redisTemplate;
    private final WebClient webClient;
    private final  AwsCognitoTokenProvider tokenProvider;

    private final ObjectMapper mapper = new ObjectMapper();

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

        Optional<RateInfoDto> cachedData = getCachedRate(currencyPairKey);
        if (cachedData.isPresent()) {
            RateInfoDto data = cachedData.get();
            return new ConversionResponseDto(
                    from, to,
                    data.getRate(),
                    amount * data.getRate(),
                    data.getSource(),
                    data.getTimestamp(),
                    StateFlag.CACHED
            );
        }

        String accessToken = tokenProvider.getAccessToken();
        Optional<RateInfoDto> historicalData = getHistoricalRate(currencyPairKey, accessToken);
        if (historicalData.isPresent()) {
            RateInfoDto data = historicalData.get();
            return new ConversionResponseDto(
                    from, to,
                    data.getRate(),
                    amount * data.getRate(),
                    data.getSource(),
                    data.getTimestamp(),
                    StateFlag.FALLBACK_DB
            );
        }

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
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
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
        try {
            // Make a blocking HTTP request to the history-service
            String responseBody = webClient.get()
                    .uri(currencyPair)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Downstream error: " + body)))
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = mapper.readTree(responseBody);

            if (node.has("rate") && node.has("source")) {
                double rate = node.get("rate").asDouble();
                String source = node.get("source").asText();
                Instant timestamp = Instant.now();
                return Optional.of(new RateInfoDto(source, timestamp, rate));
            } else {
                return Optional.empty();
            }

        } catch (WebClientResponseException e) {
            throw new RuntimeException(
                    "HTTP error fetching rate: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error fetching rate", e);
        }
    }
}