package com.vaxly.conversionservice.unit;

import com.vaxly.conversionservice.service.ConversionService;
import com.vaxly.conversionservice.dtos.ConversionResponseDto;
import com.vaxly.conversionservice.dtos.RateInfoDto;
import com.vaxly.conversionservice.enums.StateFlag;
import com.vaxly.conversionservice.security.AwsCognitoTokenProvider;
import com.vaxly.conversionservice.service.SqsProducerService;
import com.vaxly.conversionservice.service.UsageCounterService;
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

    @Mock
    UsageCounterService usageCounterService;

    @Mock
    SqsProducerService sqsProducerService;

    @Spy
    @InjectMocks
    private ConversionService conversionService;

    private final String DEFAULT_FROM = "USD";
    private final String DEFAULT_TO = "EUR";
    private final String DEFAULT_ACCESS_TOKEN = "MOCK_ACCESS_TOKEN";
    private final double DEFAULT_AMOUNT = 100.0;
    private final double DEFAULT_RATE = 1.25;
    private final String DEFAULT_SOURCE = "TEST_SOURCE";
    private final String DEFAULT_CURRENCY_PAIR = DEFAULT_FROM + "_" + DEFAULT_TO;

    @Test
    @DisplayName("should return cached data")
    public void whenConvert_thenReturnsCachedData() {
        RateInfoDto mockRateInfo = new RateInfoDto(DEFAULT_SOURCE, Instant.now(), DEFAULT_RATE);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(DEFAULT_CURRENCY_PAIR)).thenReturn(mockRateInfo);

        ConversionResponseDto result = conversionService.convert(DEFAULT_FROM, DEFAULT_TO, DEFAULT_AMOUNT);

        verify(usageCounterService, times(1)).incrementUsage(DEFAULT_CURRENCY_PAIR);

        assertEquals(DEFAULT_RATE, result.getRate());
        assertEquals(DEFAULT_SOURCE, result.getSource());
        assertEquals(StateFlag.CACHED, result.getStateFlag());
    }

    @Test
    @DisplayName("convert() returns historical rate when cache is empty")
    public void givenCacheEmpty_whenConvert_thenReturnsHistoricalRate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(DEFAULT_CURRENCY_PAIR)).thenReturn(null);

        when(tokenProvider.getAccessToken()).thenReturn(DEFAULT_ACCESS_TOKEN);

        RateInfoDto mockHistoricalRate = new RateInfoDto(DEFAULT_SOURCE, Instant.now(), DEFAULT_RATE);
        doReturn(Optional.of(mockHistoricalRate))
                .when(conversionService).getHistoricalRate(DEFAULT_CURRENCY_PAIR, DEFAULT_ACCESS_TOKEN);

        ConversionResponseDto result = conversionService.convert(DEFAULT_FROM, DEFAULT_TO, DEFAULT_AMOUNT);

        verify(usageCounterService, times(1)).incrementUsage(DEFAULT_CURRENCY_PAIR);
        assertEquals(DEFAULT_RATE, result.getRate());
        assertEquals(DEFAULT_SOURCE, result.getSource());
        assertEquals(StateFlag.FALLBACK_DB, result.getStateFlag());
    }


    @Test
    @DisplayName("convert() returns UNAVAILABLE when no cached or historical rate exists")
    public void givenNoCacheOrHistoricalData_whenConvert_thenReturnsUnavailableState() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(DEFAULT_CURRENCY_PAIR)).thenReturn(null);
        when(tokenProvider.getAccessToken()).thenReturn(DEFAULT_ACCESS_TOKEN);
        doNothing().when(sqsProducerService).sendMessage(DEFAULT_CURRENCY_PAIR);

        doReturn(Optional.empty()).when(conversionService).getHistoricalRate(DEFAULT_CURRENCY_PAIR, DEFAULT_ACCESS_TOKEN);

        ConversionResponseDto result = conversionService.convert(DEFAULT_FROM, DEFAULT_TO, DEFAULT_AMOUNT);

        verify(usageCounterService, times(0)).incrementUsage(DEFAULT_CURRENCY_PAIR);
        verify(sqsProducerService, times(1)).sendMessage(DEFAULT_CURRENCY_PAIR);

        assertEquals(0.0, result.getRate());
        assertNull(result.getSource());
        assertEquals(StateFlag.UNAVAILABLE, result.getStateFlag());
        assertNull(result.getTimestamp());
    }

}
