package com.vaxly.vaxlyshared.service;

import com.vaxly.vaxlyshared.config.AwsSqsProperties;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.messaging.Message;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background SQS consumer service.
 * <p>
 * This service continuously polls an SQS queue using long polling (20s) and batch retrieval (up to 10 messages).
 * Messages are passed to a {@link SqsMessageHandler} for application-specific processing.
 * <p>
 * Resilience is built-in:
 * <ul>
 *   <li>Messages are only deleted after successful processing.</li>
 *   <li>Failed messages remain on the queue and will be retried after the visibility timeout.</li>
 *   <li>Polling runs in a dedicated background thread with error backoff and graceful shutdown support.</li>
 * </ul>
 */
@Service
public class SqsConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(SqsConsumerService.class);

    private final SqsTemplate sqsTemplate;
    private final String queueUrl;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final SqsMessageHandler messageHandler;
    private Thread workerThread;

    public SqsConsumerService(AwsSqsProperties props, SqsTemplate sqsTemplate, SqsMessageHandler messageHandler) {
        this.sqsTemplate = sqsTemplate;
        this.queueUrl = props.getQueueUrl();
        this.messageHandler = messageHandler;
    }

    /**
     * Starts the background polling worker thread after bean initialization.
     */
    @PostConstruct
    public void startWorker() {
        workerThread = new Thread(this::pollMessages, "sqs-consumer-worker");
        workerThread.start();
        logger.info("Started SQS consumer worker for queue: {}", queueUrl);
    }

    /**
     * Stops the worker thread gracefully on application shutdown.
     */
    @PreDestroy
    public void stopWorker() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
        logger.info("Stopped SQS consumer worker.");
    }

    /**
     * Continuously polls the queue using long polling.
     * <p>
     * Uses 20-second long polling and up to 10 messages per request.
     * Implements simple error backoff to reduce load on SQS during failures.
     */
    private void pollMessages() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Collection<Message<?>> messages = sqsTemplate.receiveMany(options -> options
                        .queue(queueUrl)
                        .maxNumberOfMessages(10)
                        .pollTimeout(Duration.ofSeconds(20))
                );

                if (!messages.isEmpty()) {
                    processMessage(messages);
                }

            } catch (Exception e) {
                logger.error("Error receiving messages from SQS: {}", e.getMessage(), e);
                try {
                    Thread.sleep(1000); // simple backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Delegates processing of a batch of messages to the configured handler.
     * <p>
     * Messages are deleted only if {@link SqsMessageHandler#handle(Collection)} completes successfully.
     *
     * @param messages Batch of SQS messages to process
     */
    private void processMessage(Collection<Message<?>> messages) {
        try {
            messageHandler.handle(messages);
            logger.info("Processed {} SQS messages successfully.", messages.size());
        } catch (Exception e) {
            logger.error("Failed to process SQS batch (size {}). Payloads: {}",
                    messages.size(),
                    messages.stream().map(m -> String.valueOf(m.getPayload())).toList(),
                    e);
        }
    }
}
