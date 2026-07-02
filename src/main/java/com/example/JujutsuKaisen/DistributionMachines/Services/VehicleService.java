package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.EntityType;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Vehicle.Vehicle;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.DuplicateException;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.EntityNotFoundException;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepVehicle;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VehicleService {

    private final RepVehicle repository;
    private final LogService logService;

    public VehicleService(RepVehicle repository, LogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    // === Чтение ===

    @Transactional(readOnly = true)
    public List<Vehicle> getAll() { return repository.getAll(); }

    @Transactional(readOnly = true)
    public Vehicle getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Машина", id));
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getAvailable(LocalDate date) { return repository.getAvailable(date); }

    // === Запись ===

    @Transactional
    public void add(Vehicle vehicle) {
        try {
            repository.add(vehicle);
            logService.log("CREATE", String.valueOf(EntityType.VEHICLE), null, "admin", "regNumber=" + vehicle.getRegNumber());
        } catch (UncategorizedSQLException | DuplicateKeyException e) {
            throw new DuplicateException("Машина с номером " + vehicle.getRegNumber() + " уже существует");
        }
    }

    @Transactional
    public void update(Integer id, Vehicle vehicle) {
        if (!repository.exists(id)) throw new EntityNotFoundException("Машина", id);
        try {
            repository.update(id, vehicle);
            logService.log("UPDATE", String.valueOf(EntityType.VEHICLE), id, "admin", "Обновлена машина");
        } catch (UncategorizedSQLException e) {
            throw new DuplicateException("Машина с таким номером уже существует");
        }
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.exists(id)) throw new EntityNotFoundException("Машина", id);
        repository.delete(id);
        logService.log("DELETE", String.valueOf(EntityType.VEHICLE), id, "admin", "Удалена машина id=" + id);
    }
}