package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepMechanic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class MechanicService {

    private final RepMechanic repository;
    private final VehicleMileageService mileageService;
    private final MaintenanceService maintenanceService;

    public MechanicService(RepMechanic repository, VehicleMileageService mileageService,
                           MaintenanceService maintenanceService) {
        this.repository = repository;
        this.mileageService = mileageService;
        this.maintenanceService = maintenanceService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getInspectionsByDate(LocalDate date) {
        return repository.getInspectionsByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFuelByDate(LocalDate date) {
        return repository.getFuelByDate(date);
    }

    @Transactional
    public void inspect(Integer vehicleId, Integer driverId, String type, String result, String notes, String checkedBy, Integer mileage) {
        if (mileage != null) {
            mileageService.validateNotLessThanLast(vehicleId, mileage);
        }
        repository.saveInspection(vehicleId, driverId, type, result, notes, checkedBy);
        if ("passed".equals(result)) {
            repository.approveWaybill(vehicleId);
        }
        if (mileage != null) {
            if ("pre_trip".equals(type)) {
                repository.updateWaybillMileageBefore(vehicleId, mileage);
            } else if ("post_trip".equals(type)) {
                repository.updateWaybillMileageAfter(vehicleId, mileage);
            }
        }
        if (driverId != null) {
            repository.updateReadiness(driverId);
        }
        if (mileage != null) {
            maintenanceService.checkAndNotify(vehicleId);
        }
    }

    @Transactional
    public void fuel(Integer vehicleId, Integer driverId, double liters, Integer mileage, String phase) {
        if (mileage != null) {
            mileageService.validateNotLessThanLast(vehicleId, mileage);
        }
        repository.saveFuel(vehicleId, driverId, liters, mileage);
        if ("post_trip".equals(phase)) {
            repository.updateWaybillFuelAfter(vehicleId, liters, mileage);
        } else {
            repository.updateWaybillFuelBefore(vehicleId, liters, mileage);
        }
        if (mileage != null) {
            maintenanceService.checkAndNotify(vehicleId);
        }
    }

}