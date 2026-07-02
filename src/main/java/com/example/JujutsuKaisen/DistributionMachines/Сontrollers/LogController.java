package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.Services.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public List<Map<String, Object>> getLogs(@RequestParam(defaultValue = "100") int limit) {
        return logService.getLogs(limit);
    }
}