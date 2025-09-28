package com.vaxly.historicalservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistoricalRateRepository extends JpaRepository<HistoricalRate, Integer> {

    /**
     * Finds a HistoricalRate entity by its currencyPair.
     * Spring Data JPA automatically generates the query for this method.
     *
     * @param currencyPair the currency pair (e.g., "USD_EUR")
     * @return an Optional containing the found entity, or empty if not found
     */
     Optional<HistoricalRate> findByCurrencyPair(String currencyPair);
}
