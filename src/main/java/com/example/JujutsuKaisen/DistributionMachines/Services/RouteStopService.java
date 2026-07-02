package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.DTO.RouteStopInfo;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.DuplicateException;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepRoute;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepRouteStop;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RouteStopService {

    private final RepRouteStop repository;
    private final RepRoute routeRepository;

    public RouteStopService(RepRouteStop repository, RepRoute routeRepository) {
        this.repository = repository;
        this.routeRepository = routeRepository;
    }

    @Transactional(readOnly = true)
    public List<RouteStopInfo> getStopsByRouteId(Integer routeId) {
        return repository.getStopsByRouteId(routeId);
    }

    @Transactional
    public void addStopToRoute(Integer routeId, Integer stopId, Integer orderNum, Double distanceToNextKm, Integer timeToNextMin) {
        if (repository.existsByStopIdAndRouteId(stopId, routeId))
            throw new DuplicateException("Эта остановка уже есть на маршруте");
        if (repository.existsByOrderNum(routeId, orderNum))
            throw new DuplicateException("Остановка с порядковым номером " + orderNum + " уже есть на маршруте");
        repository.addStopToRoute(routeId, stopId, orderNum, distanceToNextKm, timeToNextMin);
        repository.reorderStops(routeId);
        routeRepository.updateStats(routeId);
    }

    @Transactional
    public void updateStopOnRoute(Integer routeId, Integer stopId, Integer orderNum, Double distanceToNextKm, Integer timeToNextMin) {
        repository.updateStopOnRoute(routeId, stopId, orderNum, distanceToNextKm, timeToNextMin);
        repository.reorderStops(routeId);
        routeRepository.updateStats(routeId);
    }

    @Transactional
    public void removeStopFromRoute(Integer routeId, Integer stopId) {
        repository.removeStopFromRoute(routeId, stopId);
        repository.reorderStops(routeId);
        routeRepository.updateStats(routeId);
    }

    @Transactional
    public void moveStop(Integer routeId, Integer stopId, int direction) {
        repository.moveStop(routeId, stopId, direction);
    }
}