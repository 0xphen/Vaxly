package com.vaxly.conversionservice.dtos;

import java.time.Instant;

public class RateInfoDto {
    private final String source;
    private final Instant timestamp;
    private final double rate;

    public RateInfoDto(String source, Instant timestamp, double rate) {
        this.source = source;
        this.timestamp = timestamp;
        this.rate = rate;
    }

    public double getRate() {
        return rate;
    }

    public String getSource() {
        return source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
