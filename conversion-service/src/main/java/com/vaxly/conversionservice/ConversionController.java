package com.vaxly.conversionservice;

import com.vaxly.conversionservice.enums.StateFlag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaxly.conversionservice.dtos.ConversionResponseDto;

@RestController
@RequestMapping("api/v1/convert")
public class ConversionController {
    private final ConversionService conversionService;
    private static final Logger logger = LoggerFactory.getLogger(ConversionController.class);

    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping
    public ResponseEntity<ConversionResponseDto> convert(@RequestParam String from, @RequestParam String to, @RequestParam double amount) {
        logger.info("Received request to convert {} {} to {}", amount, from, to);

        ConversionResponseDto response = conversionService.convert(from, to, amount);
        if (response.getStateFlag() == StateFlag.UNAVAILABLE) {
            logger.warn("Conversion service is unavailable. Returning 503 Service Unavailable.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        logger.info("Successfully converted {} {} to {} with result: {}", amount, from, to, response.getConvertedAmount());
        return ResponseEntity.ok(response);
    }
}
