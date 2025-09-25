package com.vaxly.conversionservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaxly.conversionservice.dtos.CognitoResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides AWS Cognito JWT access tokens using client credentials flow.
 * Tokens are cached in memory and refreshed proactively before expiry.
 * Thread-safe: multiple threads can call getToken() without blocking.
 */
@Component
public class AwsCognitoTokenProvider {

    private volatile CachedToken currentToken;
    private final ReentrantLock lock = new ReentrantLock();

    @Value("${security.cognito.client-id}")
    private String clientId;

    @Value("${security.cognito.client-secret}")
    private String clientSecret;

    @Value("${security.cognito.token-endpoint}")
    private String tokenEndpoint;

    @Value("${security.cognito.scope}")
    private String scope;


    /**
     * Returns a valid access token. Fast: just reads cached token.
     */
    public String getAccessToken() {
        if (currentToken == null) {
            refreshToken();
        }
        return currentToken.getAccessToken();
    }

    /**
     * Scheduled task to refresh token proactively before it expires.
     * Runs every minute (adjustable).
     */
    @Scheduled(fixedDelay = 60_000)
    private void scheduledRefresh() {
        if (currentToken != null && currentToken.isExpiredOrExpiringSoon()) {
            refreshToken();
        }
    }

    /**
     * Thread-safe token refresh using lock to prevent multiple concurrent fetches.
     */
    private void refreshToken() {
        lock.lock();
        try {
            if (currentToken != null && !currentToken.isExpiredOrExpiringSoon()) {
                return; // another thread already refreshed
            }

            CognitoResponseDto response = requestNewTokenFromCognito();
            Instant expiryTime = Instant.now().plusSeconds(response.getExpiresIn());
            currentToken = new CachedToken(response.getAccessToken(), expiryTime);

        } finally {
            lock.unlock();
        }
    }

    /**
     * Fetches a new token from Cognito using client credentials flow.
     */
    private CognitoResponseDto requestNewTokenFromCognito() {
        try {
            String body = "grant_type=client_credentials" +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&scope=" + scope;

            WebClient client = WebClient.builder()
                    .baseUrl(tokenEndpoint)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .build();

            String responseBody = client.post()
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);

            String accessToken = jsonNode.get("access_token").asText();
            long expiresIn = jsonNode.get("expires_in").asLong();

            return new CognitoResponseDto(accessToken, expiresIn);

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Failed to fetch token from Cognito: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error fetching token from Cognito", e);
        }
    }
}
