package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.EntityType;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.DriverBusyException;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.VehicleBusyException;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepDispatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class DispatcherService {

    private final RepDispatcher repository;
    private final LogService logService;
    private final VehicleMileageService mileageService;

    public DispatcherService(RepDispatcher repository, LogService logService, VehicleMileageService mileageService) {
        this.repository = repository;
        this.logService = logService;
        this.mileageService = mileageService;
    }

    // === Просмотр ===

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTripsByDate(LocalDate date) {
        return repository.getTripsByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableDrivers(LocalDate date) {
        return repository.getAvailableDrivers(date);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableVehicles(LocalDate date) {
        return repository.getAvailableVehicles(date);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWaybillByTripId(Integer tripId) {
        return repository.getWaybillByTripId(tripId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWaybillsByDate(LocalDate date) {
        return repository.getWaybillsByDate(date);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkReadiness(Integer vehicleId, Integer driverId, LocalDate date) {
        return repository.checkReadiness(vehicleId, driverId, date);
    }

    // === Действия ===

    @Transactional
    public void reassignTrip(Integer tripId, Integer vehicleId, Integer driverId) {
        try {
            repository.reassignTrip(tripId, vehicleId, driverId);
            logService.log("UPDATE", String.valueOf(EntityType.TRIP), tripId, "dispatcher", "reassign");
        } catch (Exception e) {
            logService.log("UPDATE_FAILED", String.valueOf(EntityType.TRIP), tripId, "dispatcher", e.getMessage());
            String msg = e.getMessage();
            if (msg.contains("Машина")) throw new VehicleBusyException(vehicleId, "");
            if (msg.contains("Водитель")) throw new DriverBusyException(driverId, "");
            throw new RuntimeException(msg);
        }
    }

    public List<Map<String, Object>> getMedicalByDate(LocalDate date) {
        return repository.getMedicalByDate(date);
    }

    public List<Map<String, Object>> getInspectionsByDate(LocalDate date) {
        return repository.getInspectionsByDate(date);
    }

    public List<Map<String, Object>> getFuelByDate(LocalDate date) {
        return repository.getFuelByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVehicleControl(LocalDate date) {
        return mileageService.getVehicleControl(date);
    }
}