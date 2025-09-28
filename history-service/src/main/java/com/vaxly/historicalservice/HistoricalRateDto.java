package com.vaxly.historicalservice;

import java.math.BigDecimal;
import java.time.Instant;

public class HistoricalRateDto {
    private final String currencyPair;
    private final String source;
    private final BigDecimal rate;
    private final Instant lastUpdatedAt;

    public HistoricalRateDto(String currencyPair, BigDecimal rate, Instant lastUpdatedAt, String source) {
        this.currencyPair = currencyPair;
        this.rate = rate;
        this.lastUpdatedAt = lastUpdatedAt;
        this.source = source;
    }

    public String getCurrencyPair() {
        return currencyPair;
    }
    public BigDecimal getRate() {
        return rate;
    }
    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    public String getSource() {
        return source;
    }
}
