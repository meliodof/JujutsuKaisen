package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.TripAssignment;
import com.example.JujutsuKaisen.DistributionMachines.Services.DriverService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/driver")
public class DriverCabinetController {

    private final DriverService service;

    public DriverCabinetController(DriverService service) {
        this.service = service;
    }

    @GetMapping("/trips")
    public List<TripAssignment> getTrips(@RequestParam(required = false) Integer driverId,
                                         @RequestParam(required = false) LocalDate date) {
        if (driverId == null) driverId = 1;
        return service.getTrips(driverId, date);
    }

    @GetMapping("/schedule/today")
    public List<TripAssignment> getTodaySchedule(@RequestParam(required = false) Integer driverId) {
        if (driverId == null) driverId = 1;
        return service.getTodaySchedule(driverId);
    }
}