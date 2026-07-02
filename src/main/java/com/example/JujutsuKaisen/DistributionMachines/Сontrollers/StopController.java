package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Stop;
import com.example.JujutsuKaisen.DistributionMachines.Services.StopService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final StopService service;

    public StopController(StopService service) {
        this.service = service;
    }

    // GET /api/stops — все остановки
    @GetMapping
    public List<Stop> getAll() {
        return service.getAll();
    }

    // GET /api/stops/{id} — остановка по ID
    @GetMapping("/{id}")
    public Stop getById(@PathVariable Integer id) {
        return service.getById(id);
    }
}
