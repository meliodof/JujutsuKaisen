package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.Exceptions.DuplicateException;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepMedical;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class MedicalService {

    private final RepMedical repository;

    public MedicalService(RepMedical repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getByDate(LocalDate date) {
        return repository.getByDate(date);
    }

    @Transactional
    public void check(Integer driverId, String checkType, String result, String notes, String checkedBy) {
        if (repository.existsToday(driverId, checkType)) {
            throw new DuplicateException("Водитель уже осмотрен сегодня (" + checkType + ")");
        }
        repository.save(driverId, checkType, result, notes, checkedBy);
        if ("passed".equals(result)) {
            repository.approveWaybill(driverId);
        }
        repository.updateReadiness(driverId);
    }


}