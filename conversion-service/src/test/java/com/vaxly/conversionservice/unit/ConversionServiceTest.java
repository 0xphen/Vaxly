package com.vaxly.conversionservice.unit;

import com.vaxly.conversionservice.ConversionService;
import com.vaxly.conversionservice.dtos.ConversionResponseDto;
import com.vaxly.conversionservice.dtos.RateInfoDto;
import com.vaxly.conversionservice.enums.StateFlag;
import com.vaxly.conversionservice.security.AwsCognitoTokenProvider;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ConversionServiceTest {

    @Mock
    private RedisTemplate<String, RateInfoDto> redisTemplate;

    @Mock
    private ValueOperations<String, RateInfoDto> valueOperations;

    @Mock
    AwsCognitoTokenProvider tokenProvider;

    @Spy
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

    @Test
    @DisplayName("convert() returns historical rate when cache is empty")
    public void givenCacheEmpty_whenConvert_thenReturnsHistoricalRate() {
        String accessToken = "test_token";
        String source = "test_source";
        double rate = 1.25;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("USD_EUR")).thenReturn(null);
        when(tokenProvider.getAccessToken()).thenReturn(accessToken);

        RateInfoDto mockHistoricalRate = new RateInfoDto(source, Instant.now(), rate);
        doReturn(Optional.of(mockHistoricalRate))
                .when(conversionService).getHistoricalRate("USD_EUR", accessToken);

        ConversionResponseDto result = conversionService.convert("USD", "EUR", 100.0);

        assertEquals(rate, result.getRate());
        assertEquals(source, result.getSource());
        assertEquals(StateFlag.FALLBACK_DB, result.getStateFlag());
    }


    @Test
    @DisplayName("convert() returns UNAVAILABLE when no cached or historical rate exists")
    public void givenNoCacheOrHistoricalData_whenConvert_thenReturnsUnavailableState() {
        String accessToken = "test_token";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("USD_EUR")).thenReturn(null);
        when(tokenProvider.getAccessToken()).thenReturn(accessToken);

        doReturn(Optional.empty()).when(conversionService).getHistoricalRate("USD_EUR", accessToken);

        ConversionResponseDto result = conversionService.convert("USD", "EUR", 100.0);

        assertEquals(0.0, result.getRate());
        assertNull(result.getSource());
        assertEquals(StateFlag.UNAVAILABLE, result.getStateFlag());
        assertNull(result.getTimestamp());
    }

}
