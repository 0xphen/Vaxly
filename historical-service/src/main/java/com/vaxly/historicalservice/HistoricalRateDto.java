package com.vaxly.historicalservice;

import java.math.BigDecimal;
import java.time.Instant;

public class HistoricalRateDto {
    private final String currencyPair;
    private final BigDecimal rate;
    private final Instant lastUpdatedAt;

    public HistoricalRateDto(String currencyPair, BigDecimal rate, Instant lastUpdatedAt) {
        this.currencyPair = currencyPair;
        this.rate = rate;
        this.lastUpdatedAt = lastUpdatedAt;
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
}
