package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.EntityType;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Driver;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.TripAssignment;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.DuplicateException;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.EntityNotFoundException;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepDriver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DriverService {

    private final RepDriver repository;
    private final LogService logService;

    public DriverService(RepDriver repository, LogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    // === Чтение ===

    @Transactional(readOnly = true)
    public List<Driver> getAll() { return repository.getAll(); }

    @Transactional(readOnly = true)
    public Driver getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Водитель", id));
    }

    @Transactional(readOnly = true)
    public List<Driver> getAvailable(LocalDate date) { return repository.getAvailable(date); }

    @Transactional(readOnly = true)
    public List<TripAssignment> getTrips(Integer driverId, LocalDate date) {
        return repository.getTrips(driverId, date);
    }

    @Transactional(readOnly = true)
    public List<TripAssignment> getTodaySchedule(Integer driverId) {
        return repository.getTodaySchedule(driverId);
    }

    // === Запись ===

    @Transactional
    public void add(Driver driver) {
        if (repository.existsByFullName(driver.getFullName()))
            throw new DuplicateException("Водитель с таким ФИО уже существует");
        if (repository.existsByPhone(driver.getPhone()))
            throw new DuplicateException("Водитель с таким номером телефона уже существует");
        if (driver.getPassportSeries() != null && driver.getPassportNumber() != null &&
                repository.existsByPassport(driver.getPassportSeries(), driver.getPassportNumber()))
            throw new DuplicateException("Водитель с таким паспортом уже существует");
        if (driver.getLicenseNumber() != null && repository.existsByLicenseNumber(driver.getLicenseNumber()))
            throw new DuplicateException("Водитель с таким номером В/У уже существует");
        repository.add(driver);
        logService.log("CREATE", String.valueOf(EntityType.DRIVER), null, "admin", "fullName=" + driver.getFullName());
    }

    @Transactional
    public void update(Integer id, Driver driver) {
        if (!repository.exists(id)) throw new EntityNotFoundException("Водитель", id);
        if (driver.getFullName() != null && repository.existsByFullNameExcludingId(driver.getFullName(), id))
            throw new DuplicateException("Водитель с таким ФИО уже существует");
        if (driver.getPhone() != null && repository.existsByPhoneExcludingId(driver.getPhone(), id))
            throw new DuplicateException("Водитель с таким номером телефона уже существует");
        if (driver.getPassportSeries() != null && driver.getPassportNumber() != null &&
                repository.existsByPassportExcludingId(driver.getPassportSeries(), driver.getPassportNumber(), id))
            throw new DuplicateException("Водитель с таким паспортом уже существует");
        if (driver.getLicenseNumber() != null && repository.existsByLicenseNumberExcludingId(driver.getLicenseNumber(), id))
            throw new DuplicateException("Водитель с таким номером В/У уже существует");
        repository.update(id, driver);
        logService.log("UPDATE", String.valueOf(EntityType.DRIVER), id, "admin", "Обновлён водитель");
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.exists(id)) throw new EntityNotFoundException("Водитель", id);
        repository.delete(id);
        logService.log("DELETE", String.valueOf(EntityType.DRIVER), id, "admin", "Удалён водитель id=" + id);
    }
}