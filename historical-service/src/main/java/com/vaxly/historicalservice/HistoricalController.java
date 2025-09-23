package com.vaxly.historicalservice;

import com.vaxly.historicalservice.HistoricalRateDto;
import com.vaxly.historicalservice.HistoricalService;
import com.vaxly.historicalservice.exceptions.HistoricalRateNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/historical")
public class HistoricalController {
    private final HistoricalService historicalService;

    public HistoricalController(HistoricalService historicalService) {
        this.historicalService = historicalService;
    }

    @GetMapping
    public HistoricalRateDto getHistoricalRate(@RequestParam String currencyPair) {
        return historicalService.getHistoricalRate(currencyPair)
                .orElseThrow(() -> new HistoricalRateNotFoundException("Historical rate not found for " + currencyPair));
    }

    @PostMapping
    public ResponseEntity<HistoricalRateDto> createOrUpdateHistoricalRate(@RequestBody HistoricalRateDto historicalRateDto) {
        HistoricalRate createdRate = historicalService.createOrUpdateHistoricalRate(historicalRateDto);

        HistoricalRateDto responseDto = new HistoricalRateDto(
                createdRate.getCurrencyPair(),
                createdRate.getRate(),
                createdRate.getLastUpdatedAt()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdRate.getId())
                .toUri();

        return ResponseEntity.created(location).body(responseDto);
    }
}