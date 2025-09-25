package com.vaxly.historicalservice.unit;

import com.vaxly.historicalservice.HistoricalRate;
import com.vaxly.historicalservice.HistoricalRateDto;
import com.vaxly.historicalservice.HistoricalRateRepository;
import com.vaxly.historicalservice.HistoricalRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HistoricalRateServiceTest {
    @InjectMocks
    private HistoricalRateService historicalRateService;

    @Mock
    private HistoricalRateRepository historicalRateRepository;
    @Test
    @DisplayName("getHistoricalRate returns HistoricalRate when currency pair exists in repository")
    void givenExistingCurrencyPair_whenGetHistoricalRate_thenReturnsHistoricalRate() {
        String currencyPair = "EUR_USD";
        Instant now = Instant.now();
        BigDecimal rate = BigDecimal.valueOf(1.95);
        String source = "test_source";

        HistoricalRate storedRate = new HistoricalRate(currencyPair, rate, now, source);
        ReflectionTestUtils.setField(storedRate, "id", 123);

        when(historicalRateRepository.findByCurrencyPair(currencyPair))
                .thenReturn(Optional.of(storedRate));

        Optional<HistoricalRateDto> result = historicalRateService.getHistoricalRate(currencyPair);

        assertTrue(result.isPresent(), "HistoricalRate should be present for existing currency pair");
        assertEquals(currencyPair, result.get().getCurrencyPair(), "Currency pair should match");
        assertEquals(rate, result.get().getRate(), "Rate should match");
        assertEquals(now, result.get().getLastUpdatedAt(), "LastUpdatedAt should match");
        assertEquals(source, result.get().getSource(), "Source should match");
    }

    @Test
    @DisplayName("getHistoricalRate returns empty when currency pair does not exist in repository")
    void givenNonExistingCurrencyPair_whenGetHistoricalRate_thenReturnsEmpty() {
        String currencyPair = "NON_EXISTENT_PAIR";
        when(historicalRateRepository.findByCurrencyPair(currencyPair))
                .thenReturn(Optional.empty());

        Optional<HistoricalRateDto> result = historicalRateService.getHistoricalRate(currencyPair);

        assertTrue(result.isEmpty(), "HistoricalRate should be empty for non-existing currency pair");
    }


    @Test
    @DisplayName("createOrUpdateHistoricalRate creates a new HistoricalRate when not present")
    void givenNonExistingRate_whenCreateOrUpdate_thenSavesNewRate() {
        String currencyPair = "EUR_USD";
        BigDecimal rate = BigDecimal.valueOf(1.95);
        Instant now = Instant.now();
        String source = "test_source";

        HistoricalRateDto dto = new HistoricalRateDto(currencyPair, rate, now, source);

        when(historicalRateRepository.findByCurrencyPair(currencyPair))
                .thenReturn(Optional.empty());

        HistoricalRate savedRate = new HistoricalRate(currencyPair, rate, now, source);
        ReflectionTestUtils.setField(savedRate, "id", 1);
        when(historicalRateRepository.save(any(HistoricalRate.class))).thenReturn(savedRate);

        HistoricalRate result = historicalRateService.createOrUpdateHistoricalRate(dto);

        assertEquals(currencyPair, result.getCurrencyPair());
        assertEquals(rate, result.getRate());
        assertEquals(now, result.getLastUpdatedAt());

        ArgumentCaptor<HistoricalRate> captor = ArgumentCaptor.forClass(HistoricalRate.class);
        verify(historicalRateRepository).save(captor.capture());
        HistoricalRate savedArg = captor.getValue();
        assertEquals(currencyPair, savedArg.getCurrencyPair());
        assertEquals(rate, savedArg.getRate());
        assertEquals(now, savedArg.getLastUpdatedAt());
        assertEquals(source, savedArg.getSource());
    }

    @Test
    @DisplayName("createOrUpdateHistoricalRate updates existing HistoricalRate when present")
    void givenExistingRate_whenCreateOrUpdate_thenUpdatesAndSaves() {
        String currencyPair = "EUR_USD";
        BigDecimal oldRate = BigDecimal.valueOf(1.9);
        Instant oldTime = Instant.now().minusSeconds(3600);
        String source = "test_source";

        HistoricalRate existingRate = new HistoricalRate(currencyPair, oldRate, oldTime, source);
        ReflectionTestUtils.setField(existingRate, "id", 1);

        when(historicalRateRepository.findByCurrencyPair(currencyPair))
                .thenReturn(Optional.of(existingRate));

        when(historicalRateRepository.save(any(HistoricalRate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal newRate = BigDecimal.valueOf(1.95);
        HistoricalRateDto dto = new HistoricalRateDto(currencyPair, newRate, Instant.now(), source);

        HistoricalRate result = historicalRateService.createOrUpdateHistoricalRate(dto);

        assertEquals(currencyPair, result.getCurrencyPair());
        assertEquals(newRate, result.getRate());
        assertNotNull(result.getLastUpdatedAt());
        assertEquals(1, result.getId());

        ArgumentCaptor<HistoricalRate> captor = ArgumentCaptor.forClass(HistoricalRate.class);
        verify(historicalRateRepository).save(captor.capture());
        HistoricalRate savedArg = captor.getValue();
        assertEquals(currencyPair, savedArg.getCurrencyPair());
        assertEquals(newRate, savedArg.getRate());
        assertNotNull(savedArg.getLastUpdatedAt());
    }
}
