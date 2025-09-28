package com.vaxly.vaxlyshared.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SqsConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(SqsConsumerService.class);

    public void processMessage(String payload) {
        logger.info("Processing message: {}", payload);
        // Add business logic here
    }
}
