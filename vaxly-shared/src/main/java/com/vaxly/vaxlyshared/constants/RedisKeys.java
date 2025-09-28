package com.vaxly.vaxlyshared.constants;

public final class RedisKeys {

    // Prefixes (constants)
    public static final String INFLIGHT_PREFIX = "inflight:";
    public static final String LAST_REFRESH_PREFIX = "last_refresh:";
    public static final String RATE_PREFIX = "rate:";
    public static final String USAGE_ZSET_KEY = "usage:popular_pairs";

    private RedisKeys() {
    }

    // Key generators
    public static String inflightKey(String base, String quote) {
        return INFLIGHT_PREFIX + normalizePair(base, quote);
    }

    public static String inflightKey(String currencyPair) {
        return INFLIGHT_PREFIX + currencyPair.toUpperCase();
    }

    public static String lastRefreshKey(String base, String quote) {
        return LAST_REFRESH_PREFIX + normalizePair(base, quote);
    }

    public static String lastRefreshKey(String currencyPair) {
        return LAST_REFRESH_PREFIX + currencyPair.toUpperCase();
    }

    public static String rateKey(String base, String quote) {
        return RATE_PREFIX + normalizePair(base, quote);
    }

    // Helper to keep consistent casing/formatting
    public static String normalizePair(String base, String quote) {
        return base.toUpperCase() + "_" + quote.toUpperCase();
    }
}