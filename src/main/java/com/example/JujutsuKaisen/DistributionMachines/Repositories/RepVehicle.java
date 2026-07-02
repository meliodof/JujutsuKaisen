package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Vehicle.Vehicle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RepVehicle {

    private final JdbcTemplate jdbcTemplate;

    public RepVehicle(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // === Чтение ===

    public List<Vehicle> getAll() {
        String sql = """
            SELECT v.id, v.reg_number, v.model_id, v.capacity, v.status,
                   vm.name as model_name, vm.default_capacity,
                   ft.name as fuel_type_name,
                   tt.name as transport_type_name
            FROM vehicle v
            JOIN vehicle_model vm ON v.model_id = vm.id
            JOIN fuel_type ft ON vm.fuel_type_id = ft.id
            LEFT JOIN transport_type tt ON vm.transport_type_id = tt.id
            ORDER BY v.id
        """;
        return jdbcTemplate.query(sql, this::mapVehicle);
    }

    public Optional<Vehicle> findById(Integer id) {
        String sql = """
            SELECT v.id, v.reg_number, v.model_id, v.capacity, v.status,
                   vm.name as model_name, vm.default_capacity,
                   ft.name as fuel_type_name,
                   tt.name as transport_type_name
            FROM vehicle v
            JOIN vehicle_model vm ON v.model_id = vm.id
            JOIN fuel_type ft ON vm.fuel_type_id = ft.id
            LEFT JOIN transport_type tt ON vm.transport_type_id = tt.id
            WHERE v.id = ?
        """;
        return jdbcTemplate.query(sql, this::mapVehicle, id).stream().findFirst();
    }

    public List<Vehicle> getAvailable(LocalDate date) {
        String sql = """
            SELECT v.id, v.reg_number, v.model_id, v.capacity, v.status,
                   vm.name as model_name, vm.default_capacity,
                   ft.name as fuel_type_name
            FROM vehicle v
            JOIN vehicle_model vm ON v.model_id = vm.id
            JOIN fuel_type ft ON vm.fuel_type_id = ft.id
            WHERE v.status = 'active'
            AND NOT EXISTS (
                SELECT 1 FROM trip_assignment ta
                WHERE ta.vehicle_id = v.id AND ta.assignment_date = ?
            )
            ORDER BY v.id
        """;
        return jdbcTemplate.query(sql, this::mapVehicle, date);
    }

    public List<Map<String, Object>> getModelsForSelect() {
        String sql = """
            SELECT vm.id, vm.name, vm.default_capacity,
                   COALESCE(tt.name, '—') as transport_type_name
            FROM vehicle_model vm
            LEFT JOIN transport_type tt ON vm.transport_type_id = tt.id
            ORDER BY vm.name
        """;
        return jdbcTemplate.queryForList(sql);
    }

    // === Запись ===

    public void add(Vehicle vehicle) {
        jdbcTemplate.update("CALL vehicle_add(?, ?, ?)",
                vehicle.getRegNumber(), vehicle.getModelId(),
                vehicle.getStatus() != null ? vehicle.getStatus() : "active");
    }

    public void update(Integer id, Vehicle vehicle) {
        jdbcTemplate.update("CALL vehicle_update(?, ?, ?, ?)",
                id, vehicle.getRegNumber(), vehicle.getModelId(), vehicle.getStatus());
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM vehicle WHERE id = ?", id);
    }

    // === Проверки ===

    public boolean exists(Integer id) {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vehicle WHERE id = ?", Integer.class, id);
        return c != null && c > 0;
    }

    // === Маппинг ===

    private Vehicle mapVehicle(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Vehicle v = new Vehicle();
        v.setId(rs.getInt("id"));
        v.setRegNumber(rs.getString("reg_number"));
        v.setModelId(rs.getInt("model_id"));
        v.setCapacity(rs.getInt("capacity"));
        v.setStatus(rs.getString("status"));
        v.setModelName(rs.getString("model_name"));
        v.setDefaultCapacity(rs.getInt("default_capacity"));
        v.setFuelTypeName(rs.getString("fuel_type_name"));
        v.setTransportTypeName(rs.getString("transport_type_name"));
        return v;
    }
}