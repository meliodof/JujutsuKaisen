package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.Services.MedicalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medical")
public class MedicalController {

    private final MedicalService service;

    public MedicalController(MedicalService service) { this.service = service; }

    @GetMapping
    public List<Map<String, Object>> getByDate(@RequestParam LocalDate date) { return service.getByDate(date); }

    @PostMapping("/drivers/{id}/check")
    public ResponseEntity<String> check(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        service.check(id, (String) body.get("checkType"), (String) body.get("result"),
                (String) body.get("notes"), (String) body.get("checkedBy"));
        return ResponseEntity.ok("OK");
    }
}