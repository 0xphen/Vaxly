package com.vaxly.historicalservice;

import com.vaxly.historicalservice.exceptions.HistoricalRateNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/historical-rates")
public class HistoricalController {
    private final HistoricalRateService historicalRateService;

    public HistoricalController(HistoricalRateService historicalRateService) {
        this.historicalRateService = historicalRateService;
    }

    @GetMapping("/{currencyPair}")
    @PreAuthorize("hasAuthority('SCOPE_historical-service-api/historical-rates-reader')")
    public HistoricalRateDto getHistoricalRate(@PathVariable String currencyPair) {
        return historicalRateService.getHistoricalRate(currencyPair)
                .orElseThrow(() -> new HistoricalRateNotFoundException("Historical rate not found for " + currencyPair));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_historical-service-api/historical-rates-writer')")
    public ResponseEntity<HistoricalRateDto> createOrUpdateHistoricalRate(@RequestBody HistoricalRateDto historicalRateDto) {
        HistoricalRate createdRate = historicalRateService.createOrUpdateHistoricalRate(historicalRateDto);

        HistoricalRateDto responseDto = new HistoricalRateDto(
                createdRate.getCurrencyPair(),
                createdRate.getRate(),
                createdRate.getLastUpdatedAt(),
                createdRate.getSource()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdRate.getId())
                .toUri();

        return ResponseEntity.created(location).body(responseDto);
    }
}