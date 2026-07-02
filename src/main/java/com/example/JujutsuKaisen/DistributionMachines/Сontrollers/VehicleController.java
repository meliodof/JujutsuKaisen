package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Vehicle.Vehicle;
import com.example.JujutsuKaisen.DistributionMachines.Services.MaintenanceService;
import com.example.JujutsuKaisen.DistributionMachines.Services.VehicleMileageService;
import com.example.JujutsuKaisen.DistributionMachines.Services.VehicleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService service;
    private final VehicleMileageService mileageService;
    private final MaintenanceService maintenanceService;

    public VehicleController(VehicleService service, VehicleMileageService mileageService,
                             MaintenanceService maintenanceService) {
        this.service = service;
        this.mileageService = mileageService;
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public List<Vehicle> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Vehicle getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @GetMapping("/available")
    public List<Vehicle> getAvailable(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getAvailable(date);
    }

    @GetMapping("/{id}/last-mileage")
    public Map<String, Object> getLastMileage(@PathVariable Integer id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vehicleId", id);
        result.put("lastMileage", mileageService.getLastMileage(id));
        return result;
    }

    @GetMapping("/{id}/maintenance")
    public Map<String, Object> getMaintenance(@PathVariable Integer id) {
        return maintenanceService.getSettings(id);
    }

    @PutMapping("/{id}/maintenance")
    public Map<String, Object> updateMaintenance(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer lastService = body.get("lastServiceMileage") != null
                ? ((Number) body.get("lastServiceMileage")).intValue() : null;
        Integer interval = body.get("serviceIntervalKm") != null
                ? ((Number) body.get("serviceIntervalKm")).intValue() : null;
        maintenanceService.updateSettings(id, lastService, interval);
        return maintenanceService.getSettings(id);
    }

    @PostMapping("/{id}/maintenance/complete")
    public Map<String, Object> completeMaintenance(@PathVariable Integer id) {
        maintenanceService.completeService(id);
        return maintenanceService.getSettings(id);
    }

}