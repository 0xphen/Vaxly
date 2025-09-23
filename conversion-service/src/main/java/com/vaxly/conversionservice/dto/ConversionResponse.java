package com.vaxly.conversionservice.dto;
import java.time.Instant;

import com.vaxly.conversionservice.enums.StateFlag;

public class ConversionResponse {
    private final String from;
    private final String to;
    private final double rate;
    private final double convertedAmount;
    private final String source;
    private final Instant timestamp;
    private final StateFlag stateFlag;

    public ConversionResponse(String from, String to, double rate, double convertedAmount, String source, Instant timestamp, StateFlag stateFlag) {
        this.from = from;
        this.to = to;
        this.rate = rate;
        this.convertedAmount = convertedAmount;
        this.source = source;
        this.timestamp = timestamp;
        this.stateFlag = stateFlag;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public double getRate() {
        return rate;
    }

    public double getConvertedAmount() {
        return convertedAmount;
    }

    public String getSource() {
        return source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public StateFlag getStateFlag() {
        return stateFlag;
    }
}
