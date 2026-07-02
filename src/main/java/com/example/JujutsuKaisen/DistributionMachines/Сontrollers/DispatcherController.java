package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.TripAssignment;
import com.example.JujutsuKaisen.DistributionMachines.Services.DispatcherService;
import com.example.JujutsuKaisen.DistributionMachines.Services.MaintenanceService;
import com.example.JujutsuKaisen.DistributionMachines.Services.NotificationService;
import com.example.JujutsuKaisen.DistributionMachines.Services.ScheduleService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispatcher")
public class DispatcherController {

    private final DispatcherService dispatcherService;
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;
    private final MaintenanceService maintenanceService;

    public DispatcherController(DispatcherService dispatcherService, ScheduleService scheduleService,
                                NotificationService notificationService, MaintenanceService maintenanceService) {
        this.dispatcherService = dispatcherService;
        this.scheduleService = scheduleService;
        this.notificationService = notificationService;
        this.maintenanceService = maintenanceService;
    }

    // === Рейсы ===

    @GetMapping("/trips")
    public List<Map<String, Object>> getTripsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getTripsByDate(date);
    }

    @PostMapping("/trips")
    public ResponseEntity<String> assignTrip(@RequestBody TripAssignment trip) {
        scheduleService.assignTrip(trip);
        return ResponseEntity.status(HttpStatus.CREATED).body("Рейс назначен");
    }

    @PutMapping("/trips/{id}/reassign")
    public ResponseEntity<String> reassignTrip(@PathVariable Integer id,
                                               @RequestBody Map<String, Object> request) {
        Integer vehicleId = (Integer) request.get("vehicleId");
        Integer driverId = (Integer) request.get("driverId");
        dispatcherService.reassignTrip(id, vehicleId, driverId);
        return ResponseEntity.ok("Рейс обновлён");
    }

    // === Ресурсы ===

    @GetMapping("/drivers/available")
    public List<Map<String, Object>> getAvailableDrivers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getAvailableDrivers(date);
    }

    @GetMapping("/vehicles/available")
    public List<Map<String, Object>> getAvailableVehicles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getAvailableVehicles(date);
    }

    // === Путевой лист ===

    @GetMapping("/waybill/{tripId}")
    public Map<String, Object> getWaybill(@PathVariable Integer tripId) {
        return dispatcherService.getWaybillByTripId(tripId);
    }

    @PostMapping("/trips/{id}/waybill")
    public ResponseEntity<String> createWaybill(@PathVariable Integer id) {
        scheduleService.createWaybill(id);
        return ResponseEntity.ok("Путевой лист создан");
    }

    // === Проверка готовности ===

    @GetMapping("/check-readiness")
    public Map<String, Object> checkReadiness(@RequestParam Integer vehicleId,
                                              @RequestParam Integer driverId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.checkReadiness(vehicleId, driverId, date);
    }

    // === Контроль выезда (медик, механик, путевые листы) ===

    @GetMapping("/waybills")
    public List<Map<String, Object>> getWaybills(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getWaybillsByDate(date);
    }

    @GetMapping("/medical")
    public List<Map<String, Object>> getMedical(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getMedicalByDate(date);
    }

    @GetMapping("/inspections")
    public List<Map<String, Object>> getInspections(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getInspectionsByDate(date);
    }

    @GetMapping("/fuel")
    public List<Map<String, Object>> getFuel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getFuelByDate(date);
    }

    @GetMapping("/vehicle-control")
    public List<Map<String, Object>> getVehicleControl(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getVehicleControl(date);
    }

    // === Уведомления (real-time SSE + лог) ===

    @GetMapping("/notifications/stream")
    public SseEmitter notificationStream() {
        return notificationService.subscribeStream();
    }

    @GetMapping("/notifications")
    public List<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer sinceId) {
        if (sinceId != null) {
            return notificationService.getSinceId(sinceId, limit);
        }
        return notificationService.getNotifications(limit, unreadOnly);
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Integer> unreadCount() {
        return Map.of("count", notificationService.countUnread());
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<String> markRead(@PathVariable Integer id) {
        notificationService.markRead(id);
        return ResponseEntity.ok("OK");
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<String> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok("OK");
    }

    // === ТО по пробегу ===

    @GetMapping("/maintenance/due")
    public List<Map<String, Object>> maintenanceDue() {
        return maintenanceService.getDueVehicles();
    }
}