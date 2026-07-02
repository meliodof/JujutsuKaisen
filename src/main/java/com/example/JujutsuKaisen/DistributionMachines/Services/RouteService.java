package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.EntityType;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Route;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.DuplicateException;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.EntityNotFoundException;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepRoute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RouteService {

    private final RepRoute repository;
    private final LogService logService;

    public RouteService(RepRoute repository, LogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    // === Чтение ===

    @Transactional(readOnly = true)
    public List<Route> getAll() { return repository.getAll(); }

    @Transactional(readOnly = true)
    public Route getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Маршрут", id));
    }

    // === Запись ===

    @Transactional
    public void add(Route route) {
        if (repository.existsByNumber(route.getNumber()))
            throw new DuplicateException("Маршрут с номером " + route.getNumber() + " уже существует");
        repository.add(route);
        logService.log("CREATE", String.valueOf(EntityType.ROUTE), null, "admin", "number=" + route.getNumber());
    }

    @Transactional
    public void update(Integer id, Route route) {
        if (!repository.exists(id)) throw new EntityNotFoundException("Маршрут", id);
        repository.update(id, route);
        logService.log("UPDATE", String.valueOf(EntityType.ROUTE), id, "admin", "Обновлён маршрут");
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.exists(id)) throw new EntityNotFoundException("Маршрут", id);
        repository.delete(id);
        logService.log("DELETE", String.valueOf(EntityType.ROUTE), id, "admin", "Удалён маршрут id=" + id);
    }
}