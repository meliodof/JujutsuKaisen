package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepVehicle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicle-models")
public class VehicleModelController {

    private final RepVehicle repository;

    private final JdbcTemplate jdbcTemplate;

    public VehicleModelController(RepVehicle repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return repository.getModelsForSelect();
    }


    @GetMapping("/transport-types")
    public List<Map<String, Object>> getTransportTypes() {
        String sql = "SELECT id, name FROM transport_type ORDER BY name";
        return jdbcTemplate.queryForList(sql);
    }

}