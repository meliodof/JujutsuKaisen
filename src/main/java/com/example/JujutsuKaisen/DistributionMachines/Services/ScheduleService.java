package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.ScheduleTemplate;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.TripAssignment;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.DuplicateException;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepSchedule;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleService {

    private final RepSchedule repository;
    private final VehicleMileageService mileageService;
    private final NotificationService notificationService;
    private final MaintenanceService maintenanceService;

    public ScheduleService(RepSchedule repository, VehicleMileageService mileageService,
                           NotificationService notificationService, MaintenanceService maintenanceService) {
        this.repository = repository;
        this.mileageService = mileageService;
        this.notificationService = notificationService;
        this.maintenanceService = maintenanceService;
    }

    // === Чтение ===

    @Transactional(readOnly = true)
    public List<ScheduleTemplate> getScheduleByRoute(Integer routeId, Integer dayOfWeek) {
        return repository.getScheduleByRoute(routeId, dayOfWeek);
    }

    @Transactional(readOnly = true)
    public List<TripAssignment> getDailySchedule(LocalDate date) {
        return repository.getDailySchedule(date);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllTemplates() {
        return repository.getAllTemplates();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkReadiness(Integer vehicleId, Integer driverId, LocalDate date) {
        return repository.checkReadiness(vehicleId, driverId, date);
    }

    // === Действия ===

    @Transactional
    public void assignTrip(TripAssignment trip) {
        try {
            repository.assignTrip(trip);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @Transactional
    public void updateStatus(Integer id, String status, LocalDateTime dep, LocalDateTime arr,
                             String details, Integer startMileage, Integer endMileage, String reportedBy) {
        repository.updateStatus(id, status, dep, arr, details, startMileage, endMileage);
        if (startMileage != null || endMileage != null) {
            mileageService.syncTripMileage(id, startMileage, endMileage);
            var trip = repository.getTripContext(id);
            maintenanceService.checkAndNotify((Integer) trip.get("vehicle_id"));
        }
        if ("cancelled".equals(status) && details != null && !details.isBlank()) {
            notificationService.notifyTripCancelled(id, details, reportedBy);
        }
    }

    @Transactional
    public void createWaybill(Integer tripId) {
        if (repository.existsWaybill(tripId)) {
            throw new DuplicateException("Путевой лист уже создан для этого рейса");
        }
        repository.createWaybill(tripId);
    }

    // === Шаблоны ===

    @Transactional
    public void addTemplate(ScheduleTemplate st) {
        if (repository.existsTemplate(st.getRouteId(), st.getDayOfWeek(), st.getDepartureTime()))
            throw new DuplicateException("Такой шаблон уже существует");
        repository.addTemplate(st);
    }

    @Transactional
    public void deleteTemplate(Integer id) {
        repository.deleteTemplate(id);
    }



}