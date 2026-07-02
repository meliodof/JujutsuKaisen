package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepMaintenance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MaintenanceService {

    private final RepMaintenance repository;
    private final VehicleMileageService mileageService;
    private final NotificationService notificationService;

    public MaintenanceService(RepMaintenance repository, VehicleMileageService mileageService,
                              NotificationService notificationService) {
        this.repository = repository;
        this.mileageService = mileageService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSettings(Integer vehicleId) {
        Map<String, Object> settings = repository.getByVehicleId(vehicleId);
        settings.put("currentMileage", mileageService.getLastMileage(vehicleId));
        int lastService = ((Number) settings.get("last_service_mileage")).intValue();
        int interval = ((Number) settings.get("service_interval_km")).intValue();
        Integer current = mileageService.getLastMileage(vehicleId);
        int currentVal = current != null ? current : 0;
        settings.put("nextServiceAt", lastService + interval);
        settings.put("kmUntilService", Math.max(0, lastService + interval - currentVal));
        settings.put("due", currentVal >= lastService + interval);
        return settings;
    }

    @Transactional
    public void updateSettings(Integer vehicleId, Integer lastServiceMileage, Integer intervalKm) {
        repository.updateSettings(vehicleId, lastServiceMileage, intervalKm);
    }

    @Transactional
    public void completeService(Integer vehicleId) {
        Integer current = mileageService.getLastMileage(vehicleId);
        repository.markServiceDone(vehicleId, current != null ? current : 0);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDueVehicles() {
        return repository.getDueVehicles();
    }

    @Transactional
    public void checkAndNotify(Integer vehicleId) {
        if (vehicleId == null) return;
        Integer current = mileageService.getLastMileage(vehicleId);
        if (current == null || current <= 0) return;

        Map<String, Object> settings = repository.getByVehicleId(vehicleId);
        int lastService = ((Number) settings.get("last_service_mileage")).intValue();
        int interval = ((Number) settings.get("service_interval_km")).intValue();
        int lastNotified = ((Number) settings.get("last_notified_mileage")).intValue();
        int nextDue = lastService + interval;

        if (current >= nextDue && current > lastNotified) {
            String reg = repository.getVehicleRegNumber(vehicleId).orElse("ID " + vehicleId);
            String title = "Требуется ТО: " + reg;
            String message = String.format(
                    "Пробег %d км — достигнут интервал ТО (%d км). Последнее ТО на %d км.",
                    current, interval, lastService);
            notificationService.notifyDispatcher(
                    NotificationService.TYPE_MAINTENANCE, title, message, null, vehicleId, null);
            repository.setLastNotified(vehicleId, current);
        }
    }
}
