package com.vaxly.vaxlyshared.dtos;

import java.time.Instant;

public class RateInfoDto {
    private final String source;
    private final Instant timestamp;
    private final double rate;
    private final String currencyPair;

    public RateInfoDto(String source, Instant timestamp, double rate, String currencyPair) {
        this.source = source;
        this.timestamp = timestamp;
        this.rate = rate;
        this.currencyPair = currencyPair;
    }

    public double getRate() {
        return rate;
    }

    public String getCurrencyPair() {
        return currencyPair;
    }

    public String getSource() {
        return source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}