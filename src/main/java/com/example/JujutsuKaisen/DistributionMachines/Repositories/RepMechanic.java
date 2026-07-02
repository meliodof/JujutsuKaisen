package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class RepMechanic {

    private final JdbcTemplate jdbcTemplate;

    public RepMechanic(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getInspectionsByDate(LocalDate date) {
        String sql = """
            SELECT vi.*, v.reg_number as vehicle_reg, d.full_name as driver_name
            FROM vehicle_inspection vi
            LEFT JOIN vehicle v ON vi.vehicle_id = v.id
            LEFT JOIN driver d ON vi.driver_id = d.id
            WHERE vi.inspection_date = ? ORDER BY vi.inspection_date DESC
        """;
        return jdbcTemplate.queryForList(sql, date);
    }

    public List<Map<String, Object>> getFuelByDate(LocalDate date) {
        String sql = """
            SELECT fl.*, v.reg_number as vehicle_reg, d.full_name as driver_name
            FROM fuel_log fl
            LEFT JOIN vehicle v ON fl.vehicle_id = v.id
            LEFT JOIN driver d ON fl.driver_id = d.id
            WHERE fl.fuel_date = ? ORDER BY fl.fuel_date DESC
        """;
        return jdbcTemplate.queryForList(sql, date);
    }

    public void saveInspection(Integer vehicleId, Integer driverId, String type, String result, String notes, String checkedBy) {
        String sql = "INSERT INTO vehicle_inspection (vehicle_id, driver_id, inspection_type, result, notes, checked_by, inspection_date) VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE)";
        jdbcTemplate.update(sql, vehicleId, driverId, type, result, notes, checkedBy);
    }

    public void updateWaybillMileageBefore(Integer vehicleId, Integer mileage) {
        jdbcTemplate.update(
                "UPDATE waybill SET mileage_before = COALESCE(mileage_before, ?) WHERE vehicle_id = ? AND issue_date = CURRENT_DATE AND mileage_before IS NULL",
                mileage, vehicleId);
    }

    public void updateWaybillMileageAfter(Integer vehicleId, Integer mileage) {
        jdbcTemplate.update(
                "UPDATE waybill SET mileage_after = ? WHERE vehicle_id = ? AND issue_date = CURRENT_DATE AND mileage_before IS NOT NULL",
                mileage, vehicleId);
    }

    public void saveFuel(Integer vehicleId, Integer driverId, double liters, Integer mileage) {
        String sql = "INSERT INTO fuel_log (vehicle_id, driver_id, liters, mileage) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, vehicleId, driverId, liters, mileage);
    }

    public void updateWaybillFuelBefore(Integer vehicleId, double liters, Integer mileage) {
        String sql = """
        UPDATE waybill
        SET fuel_before = COALESCE(fuel_before, ?),
            mileage_before = COALESCE(mileage_before, ?)
        WHERE vehicle_id = ? AND issue_date = CURRENT_DATE AND fuel_before IS NULL
    """;
        jdbcTemplate.update(sql, liters, mileage, vehicleId);
    }

    public void updateWaybillFuelAfter(Integer vehicleId, double liters, Integer mileage) {
        String sql = """
        UPDATE waybill
        SET fuel_after = ?, mileage_after = ?
        WHERE vehicle_id = ? AND issue_date = CURRENT_DATE
          AND fuel_before IS NOT NULL AND mileage_before IS NOT NULL
    """;
        jdbcTemplate.update(sql, liters, mileage, vehicleId);
    }

    public void approveWaybill(Integer vehicleId) {
        jdbcTemplate.update("UPDATE waybill SET inspection_ok = true WHERE vehicle_id = ? AND issue_date = CURRENT_DATE", vehicleId);
    }

    public void updateReadiness(Integer driverId) {
        jdbcTemplate.update("UPDATE waybill SET ready = (medical_ok AND inspection_ok) WHERE driver_id = ? AND issue_date = CURRENT_DATE", driverId);
    }
}