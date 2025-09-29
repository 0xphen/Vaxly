package com.vaxly.vaxlyshared.service;

import org.springframework.messaging.Message;

import java.util.Collection;

public interface SqsMessageHandler {
    void handle(Collection<Message<?>> messages) throws Exception;
}
