package com.vaxly.vaxlyshared.constants;

/**
 * Provides a centralized and consistent way to manage all Redis keys used across multiple services.
 * This class helps prevent key naming collisions and typos, ensuring data integrity across services.
 * Keys are structured using prefixes to clearly define their purpose (e.g., `inflight:`, `last_refresh:`).
 */
public final class RedisKeys {

    // Prefixes
    public static final String INFLIGHT_PREFIX = "inflight:";
    public static final String LAST_REFRESH_PREFIX = "last_refresh:";
    public static final String RATE_PREFIX = "rate:";
    public static final String USAGE_ZSET_KEY = "usage:popular_pairs";

    private RedisKeys() {}

    // Key generators
    /**
     * Generates a key for a currency pair's in-flight status.
     *
     * @param base The base currency code.
     * @param quote The quote currency code.
     * @return The Redis key for the in-flight status.
     */
    public static String inflightKey(String base, String quote) {
        return INFLIGHT_PREFIX + normalizePair(base, quote);
    }

    /**
     * Generates a key for a currency pair's in-flight status.
     *
     * @param currencyPair The currency pair string (e.g., "USD_EUR").
     * @return The Redis key for the in-flight status.
     */
    public static String inflightKey(String currencyPair) {
        return INFLIGHT_PREFIX + currencyPair.toUpperCase();
    }

    /**
     * Generates a key for a currency pair's last refresh timestamp.
     *
     * @param base The base currency code.
     * @param quote The quote currency code.
     * @return The Redis key for the last refresh timestamp.
     */
    public static String lastRefreshKey(String base, String quote) {
        return LAST_REFRESH_PREFIX + normalizePair(base, quote);
    }

    /**
     * Generates a key for a currency pair's last refresh timestamp.
     *
     * @param currencyPair The currency pair string (e.g., "USD_EUR").
     * @return The Redis key for the last refresh timestamp.
     */
    public static String lastRefreshKey(String currencyPair) {
        return LAST_REFRESH_PREFIX + currencyPair.toUpperCase();
    }

    /**
     * Generates a key for a currency pair's rate data.
     *
     * @param base The base currency code.
     * @param quote The quote currency code.
     * @return The Redis key for the rate data.
     */
    public static String rateKey(String base, String quote) {
        return RATE_PREFIX + normalizePair(base, quote);
    }

    public static String rateKey(String currencyPair) {
        return RATE_PREFIX +currencyPair.toUpperCase();
    }

    /**
     * Normalizes a currency pair string to a consistent format (uppercase, separated by underscore).
     *
     * @param base The base currency code.
     * @param quote The quote currency code.
     * @return The normalized currency pair string (e.g., "USD_EUR").
     */
    public static String normalizePair(String base, String quote) {
        return base.toUpperCase() + "_" + quote.toUpperCase();
    }
}