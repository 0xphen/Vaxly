package com.vaxly.historicalservice;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class HistoricalRateService {
    private final HistoricalRateRepository historicalRateRepository;

    public HistoricalRateService(HistoricalRateRepository historicalRateRepository) {
        this.historicalRateRepository = historicalRateRepository;
    }

    /**
     * Retrieves historical rate data from the database.
     * @param currencyPair The currency pair to retrieve (e.g., "USD_EUR").
     * @return An Optional containing the rate DTO, or empty if not found.
     */
    public Optional<HistoricalRateDto> getHistoricalRate(String currencyPair) {
        Optional<HistoricalRate> historicalRate =  historicalRateRepository.findByCurrencyPair(currencyPair);
        return historicalRate.map(rate -> new HistoricalRateDto(rate.getCurrencyPair(), rate.getRate(), rate.getLastUpdatedAt(), rate.getSource()));
    }

    /**
     * Creates a new historical rate entry or updates an existing one.
     * Ensures all database operations are atomic.
     * @param historicalRateDto The DTO containing the rate data to save.
     * @return The created or updated HistoricalRate entity.
     */
    @Transactional
    public HistoricalRate createOrUpdateHistoricalRate(HistoricalRateDto historicalRateDto) {
        Optional<HistoricalRate> existingRate = historicalRateRepository.findByCurrencyPair(historicalRateDto.getCurrencyPair());

        if (existingRate.isPresent()) {
            HistoricalRate rateToUpdate = existingRate.get();
            rateToUpdate.setRate(historicalRateDto.getRate());
            rateToUpdate.setLastUpdatedAt(Instant.now());
            rateToUpdate.setSource(historicalRateDto.getSource());
            return historicalRateRepository.save(rateToUpdate);
        } else {
            return historicalRateRepository.save(new HistoricalRate(historicalRateDto.getCurrencyPair(), historicalRateDto.getRate(), historicalRateDto.getLastUpdatedAt(), historicalRateDto.getSource()));
        }
    }
}