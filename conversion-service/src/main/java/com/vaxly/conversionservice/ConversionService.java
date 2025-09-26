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
        return new ConversionResponseDto(from, to, 0.0, 0.0, null, null, StateFlag.UNAVAILABLE);
    }

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
            logger.error("Failed to retrieve historical rate for currency pair {}. Error: {}", currencyPair, e.getMessage(), e);
            throw new DownStreamException("Failed to retrieve historical rate for currency pair " + currencyPair);
        }
    }
}