package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Driver;
import com.example.JujutsuKaisen.DistributionMachines.Services.DriverService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService service;

    public DriverController(DriverService service) {
        this.service = service;
    }

    @GetMapping
    public List<Driver> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Driver getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @GetMapping("/available")
    public List<Driver> getAvailable(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getAvailable(date);
    }
}