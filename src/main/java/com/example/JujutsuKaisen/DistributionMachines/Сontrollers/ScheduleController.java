package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.ScheduleTemplate;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.TripAssignment;
import com.example.JujutsuKaisen.DistributionMachines.Services.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    // === Просмотр ===

    @GetMapping("/route/{routeId}")
    public List<ScheduleTemplate> getByRoute(@PathVariable Integer routeId,
                                             @RequestParam(required = false) Integer dayOfWeek) {
        return service.getScheduleByRoute(routeId, dayOfWeek);
    }

    @GetMapping("/daily")
    public List<TripAssignment> getDaily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getDailySchedule(date);
    }

    @GetMapping("/all")
    public List<Map<String, Object>> getAllTemplates() {
        return service.getAllTemplates();
    }

    // === Шаблоны ===

    @PostMapping("/templates")
    public ResponseEntity<String> addTemplate(@RequestBody ScheduleTemplate st) {
        service.addTemplate(st);
        return ResponseEntity.status(HttpStatus.CREATED).body("OK");
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<String> deleteTemplate(@PathVariable Integer id) {
        service.deleteTemplate(id);
        return ResponseEntity.ok("OK");
    }
}