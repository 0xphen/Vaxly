package com.vaxly.conversionservice.service;

import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SqsProducerService {
    @Value("${cloud.aws.sqs.queue-name}")
    private String queueName;

    @Autowired
    SqsTemplate sqsTemplate;

    private static final Logger logger = LoggerFactory.getLogger(SqsProducerService.class);

    /**
     * Publishes a message to the SQS queue to trigger an asynchronous process.
     * This decouples the initial request from the background task.
     *
     * @param currencyPair The currency pair to be refreshed.
     */
    public void sendMessage(String currencyPair) {
        logger.info("Sending message to SQS '{}' with payload: {}", queueName, currencyPair);

        // Asynchronously sends the message without blocking the main thread.
        SendResult<String> sendResult =  sqsTemplate.send(sqsSendOptions -> sqsSendOptions.queue(queueName).payload(currencyPair));
        logger.info("Message sent with ID: {}", sendResult.messageId());
    }
}
