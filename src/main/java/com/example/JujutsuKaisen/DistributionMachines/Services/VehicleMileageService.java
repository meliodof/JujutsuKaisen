package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.Exceptions.InvalidMileageException;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepVehicleMileage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class VehicleMileageService {

    private final RepVehicleMileage repository;

    public VehicleMileageService(RepVehicleMileage repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Integer getLastMileage(Integer vehicleId) {
        Integer last = repository.getLastMileage(vehicleId);
        return last != null && last > 0 ? last : null;
    }

    public void validateNotLessThanLast(Integer vehicleId, Integer newMileage) {
        if (newMileage == null) return;
        Integer last = repository.getLastMileage(vehicleId);
        if (last != null && last > 0 && newMileage < last) {
            throw new InvalidMileageException(
                    "Пробег " + newMileage + " км меньше последнего зафиксированного (" + last + " км)");
        }
    }

    public void validateRange(Integer before, Integer after) {
        if (before != null && after != null && after < before) {
            throw new InvalidMileageException(
                    "Пробег после (" + after + " км) не может быть меньше пробега до (" + before + " км)");
        }
    }

    @Transactional
    public void syncTripMileage(Integer tripId, Integer startMileage, Integer endMileage) {
        if (tripId == null) return;
        Map<String, Object> trip = repository.getTripMileageFields(tripId);
        Integer vehicleId = (Integer) trip.get("vehicle_id");
        Integer existingStart = trip.get("start_mileage") != null ? ((Number) trip.get("start_mileage")).intValue() : null;

        if (startMileage != null) {
            validateNotLessThanLast(vehicleId, startMileage);
        }
        if (endMileage != null) {
            validateNotLessThanLast(vehicleId, endMileage);
            Integer startForRange = startMileage != null ? startMileage : existingStart;
            if (startForRange != null) {
                validateRange(startForRange, endMileage);
            }
        }
        repository.syncWaybillMileageFromTrip(tripId, startMileage, endMileage);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVehicleControl(LocalDate date) {
        return repository.getVehicleControlByDate(date);
    }
}
