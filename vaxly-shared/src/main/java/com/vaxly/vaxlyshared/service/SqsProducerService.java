package com.vaxly.vaxlyshared.service;

import com.vaxly.vaxlyshared.config.AwsSqsProperties;
import io.awspring.cloud.sqs.operations.MessagingOperationFailedException;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SqsProducerService {

    private final String queueUrl;
    private final SqsTemplate sqsTemplate;
    private static final Logger logger = LoggerFactory.getLogger(SqsProducerService.class);

    public SqsProducerService(AwsSqsProperties props, SqsTemplate sqsTemplate) {
        this.queueUrl = props.getQueueUrl();
        this.sqsTemplate = sqsTemplate;
    }

    /**
     * Publishes a message to the SQS queue to trigger an asynchronous process.
     * This decouples the initial request from the background task.
     *
     * @param currencyPair The currency pair to be refreshed.
     */
    public void sendMessage(String currencyPair) {
        logger.info("Sending message to SQS '{}' with payload: {}", queueUrl, currencyPair);

        try {
            SendResult<String> sendResult = sqsTemplate.send(sqsSendOptions ->
                    sqsSendOptions.queue(queueUrl).payload(currencyPair));

            logger.info("Message {} queued successfully with ID: {}", currencyPair, sendResult.messageId());
        } catch (MessagingOperationFailedException e) {
            logger.error("Failed to send message to SQS queue '{}' for currency pair '{}'. Error: {}",
                    queueUrl, currencyPair, e.getMessage(), e);
        }
    }
}
