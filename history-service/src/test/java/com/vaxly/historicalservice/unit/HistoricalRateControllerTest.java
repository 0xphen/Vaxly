package com.vaxly.historicalservice.unit;

import com.vaxly.historicalservice.HistoricalController;
import com.vaxly.historicalservice.HistoricalRateDto;
import com.vaxly.historicalservice.HistoricalRate;
import com.vaxly.historicalservice.HistoricalRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HistoricalController.class)
public class HistoricalRateControllerTest {
    @MockitoBean
    private HistoricalRateService historicalRateService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/historical-rates/{currencyPair} returns 200 OK with matching rate")
    @WithMockUser(authorities = "SCOPE_historical-service-api/historical-rates-reader")
    void givenExistingCurrencyPair_whenGetHistoricalRate_thenReturns200AndRate() throws Exception {
        String currencyPair = "EUR_USD";
        Instant t = Instant.now();
        String source = "test_source";
        HistoricalRateDto mockResponse = new HistoricalRateDto(currencyPair, BigDecimal.valueOf(1.9), t, source);

        when(historicalRateService.getHistoricalRate(currencyPair)).thenReturn(Optional.of(mockResponse));

        mockMvc.perform(get("/api/v1/historical-rates/" + currencyPair))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyPair").value(currencyPair))
                .andExpect(jsonPath("$.rate").value(1.9))
                .andExpect(jsonPath("$.lastUpdatedAt").value(t.toString()))
                .andExpect(jsonPath("$.source").value(source));
    }

    @Test
    @DisplayName("GET /api/v1/historical-rates/{currencyPair} returns 403 Forbidden when scope is invalid")
    @WithMockUser(authorities = "SCOPE_historical-service-api/historical-rates-updater")
    void givenInvalidScope_whenGetHistoricalRate_thenReturns403() throws Exception {
        String currencyPair = "EUR_USD";

        mockMvc.perform(get("/api/v1/historical-rates/" + currencyPair))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/historical-rates creates new historical rate successfully")
    @WithMockUser(authorities = "SCOPE_historical-service-api/historical-rates-writer")
    void givenValidHistoricalRateDto_whenCreateOrUpdate_thenReturnsCreated() throws Exception {
        String currencyPair = "EUR_USD";
        Instant now = Instant.now();
        BigDecimal rate = BigDecimal.valueOf(1.95);
        String source = "test_source";

        HistoricalRateDto requestDto = new HistoricalRateDto(currencyPair, rate, now, source);

        HistoricalRate mockHistoricalRate = new HistoricalRate(
                currencyPair,
                rate,
                now,
                source
        );
        ReflectionTestUtils.setField(mockHistoricalRate, "id", 123);

        when(historicalRateService.createOrUpdateHistoricalRate(any(HistoricalRateDto.class)))
                .thenReturn(mockHistoricalRate);

        mockMvc.perform(post("/api/v1/historical-rates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {
                       "currencyPair": "%s",
                       "rate": %s,
                       "lastUpdatedAt": "%s"
                     }
                     """.formatted(currencyPair, rate, now.toString()))
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.currencyPair").value(currencyPair))
                .andExpect(jsonPath("$.rate").value(rate.doubleValue()))
                .andExpect(jsonPath("$.lastUpdatedAt").value(now.toString()))
                .andExpect(jsonPath("$.source").value(source));

    }

    @Test
    @DisplayName("POST /api/v1/historical-rates returns 403 Forbidden when scope is invalid")
    @WithMockUser(authorities = "SCOPE_historical-service-api/historical-rates-reader") // wrong scope
    void givenInvalidScope_whenCreateOrUpdate_thenReturnsForbidden() throws Exception {
        String currencyPair = "EUR_USD";
        Instant now = Instant.now();
        BigDecimal rate = BigDecimal.valueOf(1.95);

        mockMvc.perform(post("/api/v1/historical-rates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                         {
                           "currencyPair": "%s",
                           "rate": %s,
                           "lastUpdatedAt": "%s"
                         }
                         """.formatted(currencyPair, rate, now.toString()))
                )
                .andExpect(status().isForbidden());
    }
}
