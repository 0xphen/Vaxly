package com.vaxly.conversionservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vaxly.conversionservice.dto.ConversionResponse;

@RestController
@RequestMapping("api/v1/convert")
public class ConversionController {

    @GetMapping
    public ConversionResponse convert(@RequestParam String from, @RequestParam String to, @RequestParam double amount) {
        return new ConversionResponse(from, to, 1.0, amount, "external", null, null);
    }
}
