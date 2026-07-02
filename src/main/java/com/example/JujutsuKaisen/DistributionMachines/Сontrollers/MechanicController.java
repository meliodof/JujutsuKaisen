package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.Services.MechanicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mechanic")
public class MechanicController {

    private final MechanicService service;

    public MechanicController(MechanicService service) { this.service = service; }

    @GetMapping("/inspections")
    public List<Map<String, Object>> getInspections(@RequestParam LocalDate date) { return service.getInspectionsByDate(date); }

    @GetMapping("/fuel")
    public List<Map<String, Object>> getFuel(@RequestParam LocalDate date) { return service.getFuelByDate(date); }

    @PostMapping("/vehicles/{id}/inspection")
    public ResponseEntity<String> inspect(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        service.inspect(id,
                body.get("driverId") != null ? ((Number) body.get("driverId")).intValue() : null,
                (String) body.get("inspectionType"),
                (String) body.get("result"),
                (String) body.get("notes"),
                (String) body.get("checkedBy"),
                body.get("mileage") != null ? ((Number) body.get("mileage")).intValue() : null);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/vehicles/{id}/fuel")
    public ResponseEntity<String> fuel(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String phase = body.get("phase") != null ? (String) body.get("phase") : "pre_trip";
        service.fuel(id,
                body.get("driverId") != null ? ((Number) body.get("driverId")).intValue() : null,
                ((Number) body.get("liters")).doubleValue(),
                body.get("mileage") != null ? ((Number) body.get("mileage")).intValue() : null,
                phase);
        return ResponseEntity.ok("OK");
    }

}