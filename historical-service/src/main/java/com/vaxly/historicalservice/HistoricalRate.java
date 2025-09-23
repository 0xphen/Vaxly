package com.vaxly.historicalservice;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "historical_rates")
public class HistoricalRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String currencyPair;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(nullable = false)
    private Instant lastUpdatedAt;

    public HistoricalRate() {}

    public HistoricalRate(String currencyPair, BigDecimal rate, Instant lastUpdatedAt) {
        this.currencyPair = currencyPair;
        this.rate = rate;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Integer getId() {
        return id;
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

    void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}