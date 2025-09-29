package com.vaxly.workerservice.workerservice.service;

import com.vaxly.vaxlyshared.aws.AwsCognitoTokenProvider;
import com.vaxly.vaxlyshared.constants.RedisKeys;
import com.vaxly.vaxlyshared.dtos.RateInfoDto;
import com.vaxly.vaxlyshared.service.SqsMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SqsConsumerHandler implements SqsMessageHandler {

    @Autowired
    private RedisTemplate<String, RateInfoDto> redisTemplate;
    private final AwsCognitoTokenProvider tokenProvider;
    private final WebClient webClient;

    private static final Logger logger = LoggerFactory.getLogger(SqsConsumerHandler.class);

    public SqsConsumerHandler(RedisTemplate<String, RateInfoDto> redisTemplate, AwsCognitoTokenProvider tokenProvider, WebClient webClient) {
        this.redisTemplate = redisTemplate;
        this.tokenProvider = tokenProvider;
        this.webClient = webClient;
    }

    @Override
    public void handle(Collection<Message<?>> messages) {
        List<String> messagePayloads = messages.stream()
                .map(msg -> {
                    msg.getPayload();
                    return msg.getPayload().toString();
                })
                .toList();

        List<RateInfoDto> result = fetchRatesFromAggregator(messagePayloads);
    }

    private  List<RateInfoDto> fetchRatesFromAggregator(List<String> currencyPairs) {
        // TODO: put real business logic here
        return Collections.emptyList();
    }

    private void writeRatesToRedis(List<RateInfoDto> rates) {
        if (rates == null || rates.isEmpty()) {
            logger.info("No rates to write to Redis.");
            return;
        }

        for (RateInfoDto rate : rates) {
            try {
                String key = RedisKeys.rateKey(rate.getCurrencyPair());
                redisTemplate.opsForValue().set(key, rate);
            } catch (Exception e) {
                logger.error("Failed to write rate {} to Redis", rate, e);
            }
        }

        logger.info("Successfully wrote {} rates to Redis.", rates.size());
    }

    /**
     * Stub method to write a rate to an external history service.
     * Currently just logs the action; to be implemented with actual HTTP call.
     */
    private void writeRateToHistoryService(RateInfoDto rate) {
        if (rate == null) {
            logger.warn("Attempted to write null rate to history service.");
            return;
        }

        // TODO: Replace this with actual HTTP/gRPC client call to the history service
        logger.info("Writing rate to external history service: {}", rate);
    }
}
