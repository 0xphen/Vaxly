package com.vaxly.conversionservice.unit;

import com.vaxly.conversionservice.ConversionService;
import com.vaxly.conversionservice.dtos.ConversionResponseDto;
import com.vaxly.conversionservice.dtos.RateInfoDto;
import com.vaxly.conversionservice.enums.StateFlag;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ConversionServiceTest {

    @Mock
    private RedisTemplate<String, RateInfoDto> redisTemplate;

    @Mock
    private ValueOperations<String, RateInfoDto> valueOperations;

    @InjectMocks
    private ConversionService conversionService;

    @Test
    @DisplayName("should return cached data")
    public void whenConvert_thenReturnsCachedData() {
        RateInfoDto mockRateInfo = new RateInfoDto("redis", Instant.now(), 1.12);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("USD_EUR")).thenReturn(mockRateInfo);

        ConversionResponseDto result = conversionService.convert("USD", "EUR", 100.0);

        assertEquals(1.12, result.getRate());
        assertEquals("redis", result.getSource());
        assertEquals(StateFlag.CACHED, result.getStateFlag());
    }

//    @Test
//    @DisplayName("should return historical data")
//    public void whenConvert_thenReturnsHistoricalData() {
//        // TODO: mock call to historical-service to return Optional with data
//    }

//    @Test
//    @DisplayName("should return UNAVAILABLE state when no rate is found in cache or history")
//    public void givenNoDataSources_whenConvert_thenStateIsUnavailable() {
//        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
//        when(valueOperations.get("USD_EUR")).thenReturn(null);
//
//        // TODO: mock call to historical-service to return empty Optional
//    }
}
