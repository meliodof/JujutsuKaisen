package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.DTO.*;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Vehicle.Vehicle;
import com.example.JujutsuKaisen.DistributionMachines.Security.AppUser;
import com.example.JujutsuKaisen.DistributionMachines.Security.PasswordService;
import com.example.JujutsuKaisen.DistributionMachines.Services.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final RouteService routeService;
    private final StopService stopService;
    private final UserService userService;
    private final PasswordService passwordService;
    private final DispatcherService dispatcherService;

    public AdminController(VehicleService vs, DriverService ds, RouteService rs,
                           StopService ss, UserService us, PasswordService ps, DispatcherService dispatcherService) {
        this.vehicleService = vs;
        this.driverService = ds;
        this.routeService = rs;
        this.stopService = ss;
        this.userService = us;
        this.passwordService = ps;
        this.dispatcherService = dispatcherService;
    }

    // === Машины ===

    @PostMapping("/vehicles")
    public ResponseEntity<String> addVehicle(@RequestBody Vehicle v) {
        vehicleService.add(v);
        return ResponseEntity.status(HttpStatus.CREATED).body("Машина добавлена");
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<String> updateVehicle(@PathVariable Integer id, @RequestBody Vehicle v) {
        vehicleService.update(id, v);
        return ResponseEntity.ok("Машина обновлена");
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<String> deleteVehicle(@PathVariable Integer id) {
        vehicleService.delete(id);
        return ResponseEntity.ok("Машина удалена");
    }

    // === Водители ===

    @PostMapping("/drivers")
    public ResponseEntity<String> addDriver(@RequestBody Driver d) {
        driverService.add(d);
        return ResponseEntity.status(HttpStatus.CREATED).body("Водитель добавлен");
    }

    @PutMapping("/drivers/{id}")
    public ResponseEntity<String> updateDriver(@PathVariable Integer id, @RequestBody Driver d) {
        driverService.update(id, d);
        return ResponseEntity.ok("Водитель обновлён");
    }

    @DeleteMapping("/drivers/{id}")
    public ResponseEntity<String> deleteDriver(@PathVariable Integer id) {
        driverService.delete(id);
        return ResponseEntity.ok("Водитель удалён");
    }

    // === Маршруты ===

    @PostMapping("/routes")
    public ResponseEntity<String> addRoute(@RequestBody Route r) {
        routeService.add(r);
        return ResponseEntity.status(HttpStatus.CREATED).body("Маршрут добавлен");
    }

    @PutMapping("/routes/{id}")
    public ResponseEntity<String> updateRoute(@PathVariable Integer id, @RequestBody Route r) {
        routeService.update(id, r);
        return ResponseEntity.ok("Маршрут обновлён");
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<String> deleteRoute(@PathVariable Integer id) {
        routeService.delete(id);
        return ResponseEntity.ok("Маршрут удалён");
    }

    // === Остановки ===

    @PostMapping("/stops")
    public ResponseEntity<String> addStop(@RequestBody Stop s) {
        stopService.add(s);
        return ResponseEntity.status(HttpStatus.CREATED).body("Остановка добавлена");
    }

    @PutMapping("/stops/{id}")
    public ResponseEntity<String> updateStop(@PathVariable Integer id, @RequestBody Stop s) {
        stopService.update(id, s);
        return ResponseEntity.ok("Остановка обновлена");
    }

    @DeleteMapping("/stops/{id}")
    public ResponseEntity<String> deleteStop(@PathVariable Integer id) {
        stopService.delete(id);
        return ResponseEntity.ok("Остановка удалена");
    }

    // === Пользователи ===

    @GetMapping("/users")
    public List<AppUser> getAllUsers() { return userService.getAll(); }

    @PostMapping("/users")
    public ResponseEntity<String> addUser(@RequestBody AppUser user) {
        user.setPassword(passwordService.encode(user.getPassword()));
        userService.add(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("Пользователь создан");
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Integer id, @RequestBody AppUser user) {
        userService.update(id, user);
        return ResponseEntity.ok("Пользователь обновлён");
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.ok("Пользователь удалён");
    }

    @GetMapping("/medical")
    public List<Map<String, Object>> getMedical(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getMedicalByDate(date);
    }

    @GetMapping("/inspections")
    public List<Map<String, Object>> getInspections(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getInspectionsByDate(date);
    }

    @GetMapping("/fuel")
    public List<Map<String, Object>> getFuel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherService.getFuelByDate(date);
    }
}