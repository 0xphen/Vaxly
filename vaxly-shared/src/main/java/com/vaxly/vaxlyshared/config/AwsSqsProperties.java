package com.vaxly.vaxlyshared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.cloud.aws.sqs")
public class AwsSqsProperties {

    /**
     * The URL of the SQS queue.
     * This will be loaded from application.properties or application.yml
     */
    private String queueUrl;

    public String getQueueUrl() {
        return queueUrl;
    }

    public void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    @Override
    public String toString() {
        return "AwsSqsProperties{" +
                "queueUrl='" + queueUrl + '\'' +
                '}';
    }
}
