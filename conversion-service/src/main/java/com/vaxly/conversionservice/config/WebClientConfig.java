package com.vaxly.conversionservice.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    private static final String CORRELATION_ID = "X-Correlation-ID";

    @Bean
    public WebClient externalApiClient(WebClient.Builder webClientBuilder, ExternalApiProperties props) {
        return webClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(addCorrelationIdToRequest())
                .build();
    }

    private ExchangeFilterFunction addCorrelationIdToRequest() {
        return (request, next) ->
                Mono.deferContextual(contextView -> {
                    String correlationId = contextView.getOrDefault("correlationId", "N/A");
                    MDC.put(CORRELATION_ID, correlationId);

                    ClientRequest mutatedRequest = ClientRequest.from(request)
                            .header(CORRELATION_ID, correlationId)
                            .build();

                    return next.exchange(mutatedRequest)
                            .doFinally(signal -> MDC.clear());
                });
    }
}