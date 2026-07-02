package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RepMaintenance {

    private final JdbcTemplate jdbcTemplate;

    public RepMaintenance(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureExists(Integer vehicleId) {
        jdbcTemplate.update("""
            INSERT INTO vehicle_maintenance (vehicle_id)
            SELECT ? WHERE NOT EXISTS (SELECT 1 FROM vehicle_maintenance WHERE vehicle_id = ?)
        """, vehicleId, vehicleId);
    }

    public Map<String, Object> getByVehicleId(Integer vehicleId) {
        ensureExists(vehicleId);
        return jdbcTemplate.queryForMap(
                "SELECT * FROM vehicle_maintenance WHERE vehicle_id = ?", vehicleId);
    }

    public void updateSettings(Integer vehicleId, Integer lastServiceMileage, Integer intervalKm) {
        ensureExists(vehicleId);
        jdbcTemplate.update("""
            UPDATE vehicle_maintenance
            SET last_service_mileage = COALESCE(?, last_service_mileage),
                service_interval_km = COALESCE(?, service_interval_km),
                updated_at = NOW()
            WHERE vehicle_id = ?
        """, lastServiceMileage, intervalKm, vehicleId);
    }

    public void markServiceDone(Integer vehicleId, Integer mileageAtService) {
        ensureExists(vehicleId);
        jdbcTemplate.update("""
            UPDATE vehicle_maintenance
            SET last_service_mileage = ?,
                last_notified_mileage = 0,
                updated_at = NOW()
            WHERE vehicle_id = ?
        """, mileageAtService, vehicleId);
    }

    public void setLastNotified(Integer vehicleId, Integer mileage) {
        jdbcTemplate.update(
                "UPDATE vehicle_maintenance SET last_notified_mileage = ?, updated_at = NOW() WHERE vehicle_id = ?",
                mileage, vehicleId);
    }

    public List<Map<String, Object>> getDueVehicles() {
        String sql = """
            SELECT v.id AS vehicle_id, v.reg_number,
                   vm.last_service_mileage, vm.service_interval_km, vm.last_notified_mileage,
                   GREATEST(
                       COALESCE((SELECT MAX(mileage) FROM fuel_log WHERE vehicle_id = v.id), 0),
                       COALESCE((SELECT MAX(mileage_after) FROM waybill WHERE vehicle_id = v.id), 0),
                       COALESCE((SELECT MAX(mileage_before) FROM waybill WHERE vehicle_id = v.id), 0),
                       COALESCE((SELECT MAX(end_mileage) FROM trip_assignment WHERE vehicle_id = v.id), 0),
                       COALESCE((SELECT MAX(start_mileage) FROM trip_assignment WHERE vehicle_id = v.id), 0)
                   ) AS current_mileage
            FROM vehicle v
            JOIN vehicle_maintenance vm ON vm.vehicle_id = v.id
            WHERE v.status = 'active'
        """;
        return jdbcTemplate.queryForList(sql).stream()
                .filter(row -> {
                    int current = ((Number) row.get("current_mileage")).intValue();
                    int lastService = ((Number) row.get("last_service_mileage")).intValue();
                    int interval = ((Number) row.get("service_interval_km")).intValue();
                    return current >= lastService + interval;
                })
                .toList();
    }

    public Optional<String> getVehicleRegNumber(Integer vehicleId) {
        List<String> list = jdbcTemplate.query(
                "SELECT reg_number FROM vehicle WHERE id = ?",
                (rs, i) -> rs.getString("reg_number"), vehicleId);
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0));
    }
}
