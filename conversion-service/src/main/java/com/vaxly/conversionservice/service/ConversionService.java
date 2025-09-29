package com.vaxly.conversionservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaxly.conversionservice.dtos.ConversionResponseDto;
import com.vaxly.vaxlyshared.dtos.RateInfoDto;
import com.vaxly.vaxlyshared.service.SqsProducerService;
import com.vaxly.conversionservice.enums.StateFlag;
import com.vaxly.conversionservice.exceptions.DownStreamException;
import com.vaxly.conversionservice.exceptions.HistoricalRateNotFoundException;
import com.vaxly.vaxlyshared.aws.AwsCognitoTokenProvider;
import com.vaxly.vaxlyshared.constants.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
public class ConversionService {

    @Autowired
    private RedisTemplate<String, RateInfoDto> redisTemplate;

    private final WebClient webClient;
    private final  AwsCognitoTokenProvider tokenProvider;
    private final SqsProducerService sqsProducerService;
    private final UsageCounterService usageCounterService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(ConversionService.class);

    public ConversionService(RedisTemplate<String, RateInfoDto> redisTemplate, WebClient webClient, AwsCognitoTokenProvider tokenProvider, SqsProducerService sqsProducerService, UsageCounterService usageCounterService) {
        this.redisTemplate = redisTemplate;
        this.webClient = webClient;
        this.tokenProvider = tokenProvider;
        this.sqsProducerService = sqsProducerService;
        this.usageCounterService = usageCounterService;
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
        String currencyPair = RedisKeys.normalizePair(from, to);
        logger.info("Starting conversion for {} from {} to {}. Checking cache for key: {}", amount, from, to, currencyPair);

        Optional<RateInfoDto> cachedData = getCachedRate(currencyPair);
        if (cachedData.isPresent()) {
            usageCounterService.incrementUsage(currencyPair);

            RateInfoDto data = cachedData.get();
            logger.info("Rate for {} found in cache. Source: {}", currencyPair, data.getSource());
            return new ConversionResponseDto(
                    from, to,
                    data.getRate(),
                    amount * data.getRate(),
                    data.getSource(),
                    data.getTimestamp(),
                    StateFlag.CACHED
            );
        }

        logger.info("Rate for {} not found in cache. Falling back to external API.", currencyPair);
        String accessToken = tokenProvider.getAccessToken();
        Optional<RateInfoDto> historicalData = getHistoricalRate(currencyPair, accessToken);
        if (historicalData.isPresent()) {
            usageCounterService.incrementUsage(currencyPair);

            RateInfoDto data = historicalData.get();
            logger.info("Successfully fetched rate for {} from external API. Source: {}", currencyPair, data.getSource());
            return new ConversionResponseDto(
                    from, to,
                    data.getRate(),
                    amount * data.getRate(),
                    data.getSource(),
                    data.getTimestamp(),
                    StateFlag.FALLBACK_DB
            );
        }

        logger.warn("Rate for {} not found in cache or external API. Returning UNAVAILABLE status.", currencyPair);

        // Publish a background refresh message for this currency pair to trigger async rate population
        sqsProducerService.sendMessage(currencyPair);
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
     * This method coordinates the retrieval of a currency rate by handling the
     * primary call and managing potential exceptions.
     *
     * @param currencyPair The currency pair string (e.g., "USD_EUR").
     * @param accessToken  The AWS Cognito access token for authorization.
     * @return An {@link Optional} containing the {@link RateInfoDto} if the rate is retrieved,
     * or empty if the request failed or returned no data.
     */
    public Optional<RateInfoDto> getHistoricalRate(String currencyPair, String accessToken) {
        logger.info("Fetching historical rate from external API for pair: {}", currencyPair);

        try {
            Optional<RateInfoDto> result = fetchRateFromApi(currencyPair, accessToken);

            if (result.isPresent()) {
                logger.info("Successfully received rate for {} from downstream API. Rate: {}", currencyPair, result.get().getRate());
            } else {
                logger.warn("Response from downstream API for {} did not contain a valid rate.", currencyPair);
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to retrieve historical rate for currency pair {}. Error: {}", currencyPair, e.getMessage(), e);
            throw new DownStreamException("Failed to retrieve historical rate for currency pair " + currencyPair);
        }
    }

    /**
     * Executes the WebClient call and handles deserialization and HTTP status codes.
     * <p>
     * This helper method encapsulates the entire WebClient interaction, returning an
     * Optional with the result or an empty Optional on a 404 response. Other errors
     * will be propagated as exceptions.
     *
     * @param currencyPair The currency pair for the API call.
     * @param accessToken  The authorization token.
     * @return An {@link Optional} containing the deserialized data or empty on a 404 response.
     */
    private Optional<RateInfoDto> fetchRateFromApi(String currencyPair, String accessToken) {
        try {
            String responseBody = webClient.get()
                    .uri(currencyPair)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), resp -> {
                        logger.warn("Rate for {} not found in downstream service (404).", currencyPair);
                        return Mono.error(new HistoricalRateNotFoundException("Rate not found"));
                    })
                    .onStatus(HttpStatusCode::isError, resp -> {
                        logger.error("Downstream service returned an error. Status: {}", resp.statusCode());
                        return resp.createException();
                    })
                    .bodyToMono(String.class)
                    .block();


            JsonNode node = mapper.readTree(responseBody);
            if (node.has("rate") && node.has("source")) {
                double rate = node.get("rate").asDouble();
                String source = node.get("source").asText();
                Instant timestamp = Instant.now();
                return Optional.of(new RateInfoDto(source, timestamp, rate));
            } else {
                logger.warn("Response from downstream API for {} did not contain expected fields.", currencyPair);
                return Optional.empty();
            }
        } catch (HistoricalRateNotFoundException e) {
            return Optional.empty();
        } catch (WebClientResponseException e) {
            logger.error("WebClient error while fetching historical rate for {}. Status: {}, Body: {}", currencyPair, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to retrieve historical rate for currency pair {}. Error: {}", currencyPair, e.getMessage(), e);
            return Optional.empty();
        }
    }
}