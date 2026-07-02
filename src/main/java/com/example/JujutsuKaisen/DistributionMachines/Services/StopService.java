package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.EntityType;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Stop;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.DuplicateException;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.EntityNotFoundException;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepStop;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StopService {

    private final RepStop repository;
    private final LogService logService;

    public StopService(RepStop repository, LogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public List<Stop> getAll() {
        return repository.getAll();
    }

    @Transactional(readOnly = true)
    public Stop getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Остановка", id));
    }

    @Transactional
    public void add(Stop stop) {
        if (stop.getLatitude() != null && stop.getLongitude() != null &&
                repository.existsByCoordinates(stop.getLatitude(), stop.getLongitude()))
            throw new DuplicateException("Остановка с такими координатами уже существует");
        repository.add(stop);
        logService.log("CREATE", String.valueOf(EntityType.STOP), null, "admin", "name=" + stop.getName());
    }

    @Transactional
    public void update(Integer id, Stop stop) {
        if (!repository.exists(id)) throw new EntityNotFoundException("Остановка", id);
        if (stop.getLatitude() != null && stop.getLongitude() != null &&
                repository.existsByCoordinatesExcludingId(stop.getLatitude(), stop.getLongitude(), id))
            throw new DuplicateException("Остановка с такими координатами уже существует");
        repository.update(id, stop);
        logService.log("UPDATE", String.valueOf(EntityType.STOP), id, "admin", "Обновлена остановка");
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.exists(id)) throw new EntityNotFoundException("Остановка", id);
        repository.delete(id);
        logService.log("DELETE", String.valueOf(EntityType.STOP), id, "admin", "Удалена остановка id=" + id);
    }
}