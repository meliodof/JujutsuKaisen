package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.Services.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class StatusController {

    private final ScheduleService service;

    public StatusController(ScheduleService service) {
        this.service = service;
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        String status = (String) body.get("status");
        LocalDateTime dep = parse(body.get("actualDeparture"));
        LocalDateTime arr = parse(body.get("actualArrival"));
        String details = (String) body.get("details");
        Integer startMileage = body.get("startMileage") != null ? ((Number) body.get("startMileage")).intValue() : null;
        Integer endMileage = body.get("endMileage") != null ? ((Number) body.get("endMileage")).intValue() : null;
        String reportedBy = (String) body.get("reportedBy");
        if (reportedBy == null || reportedBy.isBlank()) {
            reportedBy = request.getHeader("X-Username");
        }

        service.updateStatus(id, status, dep, arr, details, startMileage, endMileage, reportedBy);
        return ResponseEntity.ok("Статус обновлён");
    }

    private LocalDateTime parse(Object value) {
        if (value == null) return null;
        return Instant.parse((String) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}