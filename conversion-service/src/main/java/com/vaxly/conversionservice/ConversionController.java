package com.vaxly.conversionservice;

import com.vaxly.conversionservice.enums.StateFlag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vaxly.conversionservice.dtos.ConversionResponseDto;

@RestController
@RequestMapping("api/v1/convert")
public class ConversionController {
    private final ConversionService conversionService;

    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping
    public ResponseEntity<ConversionResponseDto> convert(@RequestParam String from, @RequestParam String to, @RequestParam double amount) {
        ConversionResponseDto response = conversionService.convert(from, to, amount);
        if (response.getStateFlag() == StateFlag.UNAVAILABLE) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
