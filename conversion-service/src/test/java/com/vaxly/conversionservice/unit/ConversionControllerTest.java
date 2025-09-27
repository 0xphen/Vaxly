package com.vaxly.conversionservice.unit;

import com.vaxly.conversionservice.ConversionController;
import com.vaxly.conversionservice.service.ConversionService;
import com.vaxly.conversionservice.dtos.ConversionResponseDto;
import com.vaxly.conversionservice.enums.StateFlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionController.class)
public class ConversionControllerTest {
    @MockitoBean
    private ConversionService conversionService;

    @Autowired
    private MockMvc mockMvc;


    @Test
    @DisplayName("should return 200 OK and valid conversion data when service is available")
    public void whenGetConvertEndpoint_thenReturnsSuccessfulConversionResponse() throws Exception {
        ConversionResponseDto mockResponse = new ConversionResponseDto( "USD", "EUR", 0.92, 92.0, "external", null, StateFlag.CACHED);

        when(conversionService.convert("USD", "EUR", 100.0)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/convert?from=USD&to=EUR&amount=100.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.convertedAmount").value(92))
                .andExpect(jsonPath("$.stateFlag").value("CACHED"));
    }

    @Test
    @DisplayName("should return 503 SERVICE UNAVAILABLE when data is unavailable")
    void whenGetConvertEndpoint_thenReturnsServiceUnavailable() throws Exception {
        ConversionResponseDto unavailableResponse = new ConversionResponseDto(
                "USD", "EUR", 0.0, 0.0, null, null, StateFlag.UNAVAILABLE
        );
        when(conversionService.convert("USD", "EUR", 100.0)).thenReturn(unavailableResponse);

        // Perform the request and assert a 503 status
        mockMvc.perform(get("/api/v1/convert?from=USD&to=EUR&amount=100.0"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.stateFlag").value("UNAVAILABLE"));
    }
}
