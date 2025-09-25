package com.vaxly.conversionservice.security;

import java.time.Instant;

/**
 * A container to hold a cached access token and its expiry time.
 * This class is designed to support proactive token refreshing.
 */
public class CachedToken {

    private final String accessToken;
    private final Instant expiry;

    private static final long REFRESH_BUFFER_SECONDS = 300;

    public CachedToken(String accessToken, Instant expiry) {
        this.accessToken = accessToken;
        this.expiry = expiry;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Instant getExpiry() {
        return expiry;
    }

    /**
     * Checks if the token is expired or will expire soon, based on a buffer.
     *
     * @return true if the token is expired or within the buffer, false otherwise.
     */
    public boolean isExpiredOrExpiringSoon() {
        return Instant.now().isAfter(expiry.minusSeconds(REFRESH_BUFFER_SECONDS));
    }

    /**
     * Overload to allow custom buffer if needed.
     */
    public boolean isExpiredOrExpiringSoon(long bufferSeconds) {
        return Instant.now().isAfter(expiry.minusSeconds(bufferSeconds));
    }
}