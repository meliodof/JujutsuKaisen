package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Route;
import com.example.JujutsuKaisen.DistributionMachines.DTO.RouteStopInfo;
import com.example.JujutsuKaisen.DistributionMachines.Services.RouteService;
import com.example.JujutsuKaisen.DistributionMachines.Services.RouteStopService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService service;
    private final RouteStopService routeStopService;

    public RouteController(RouteService service, RouteStopService routeStopService) {
        this.service = service;
        this.routeStopService = routeStopService;
    }

    // === Маршруты ===

    @GetMapping
    public List<Route> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public Route getById(@PathVariable Integer id) { return service.getById(id); }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody Route r) {
        service.add(r);
        return ResponseEntity.status(HttpStatus.CREATED).body("Маршрут добавлен");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Integer id, @RequestBody Route r) {
        service.update(id, r);
        return ResponseEntity.ok("Маршрут обновлён");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok("Маршрут удалён");
    }

    // === Остановки маршрута ===

    @GetMapping("/{routeId}/stops")
    public List<RouteStopInfo> getStops(@PathVariable Integer routeId) {
        return routeStopService.getStopsByRouteId(routeId);
    }

    @PostMapping("/{routeId}/stops")
    public ResponseEntity<String> addStop(@PathVariable Integer routeId, @RequestBody Map<String, Object> body) {
        routeStopService.addStopToRoute(routeId,
                ((Number) body.get("stopId")).intValue(),
                ((Number) body.get("orderNum")).intValue(),
                body.get("distanceToNextKm") != null ? ((Number) body.get("distanceToNextKm")).doubleValue() : null,
                body.get("timeToNextMin") != null ? ((Number) body.get("timeToNextMin")).intValue() : null);
        return ResponseEntity.ok("Остановка добавлена");
    }

    @PutMapping("/{routeId}/stops/{stopId}")
    public ResponseEntity<String> updateStop(@PathVariable Integer routeId, @PathVariable Integer stopId,
                                             @RequestBody Map<String, Object> body) {
        routeStopService.updateStopOnRoute(routeId, stopId,
                body.get("orderNum") != null ? ((Number) body.get("orderNum")).intValue() : null,
                body.get("distanceToNextKm") != null ? ((Number) body.get("distanceToNextKm")).doubleValue() : null,
                body.get("timeToNextMin") != null ? ((Number) body.get("timeToNextMin")).intValue() : null);
        return ResponseEntity.ok("Остановка обновлена");
    }

    @DeleteMapping("/{routeId}/stops/{stopId}")
    public ResponseEntity<String> removeStop(@PathVariable Integer routeId, @PathVariable Integer stopId) {
        routeStopService.removeStopFromRoute(routeId, stopId);
        return ResponseEntity.ok("Остановка удалена");
    }

    @PutMapping("/{routeId}/stops/{stopId}/move")
    public ResponseEntity<String> moveStop(@PathVariable Integer routeId, @PathVariable Integer stopId,
                                           @RequestParam int direction) {
        routeStopService.moveStop(routeId, stopId, direction);
        return ResponseEntity.ok("OK");
    }
}