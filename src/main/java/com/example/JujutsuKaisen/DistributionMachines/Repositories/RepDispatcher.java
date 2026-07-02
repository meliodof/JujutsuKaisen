package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RepDispatcher {

    private final JdbcTemplate jdbcTemplate;

    public RepDispatcher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // === Назначение рейсов ===

    public void reassignTrip(Integer tripId, Integer vehicleId, Integer driverId) {
        jdbcTemplate.update("CALL trip_reassign(?, ?, ?)", tripId, vehicleId, driverId);
    }

    public List<Map<String, Object>> getAvailableDrivers(LocalDate date) {
        String sql = """
            SELECT d.id, d.full_name, d.experience_years, d.license_category, d.phone
            FROM driver d
            WHERE NOT EXISTS (
                SELECT 1 FROM trip_assignment ta
                WHERE ta.driver_id = d.id AND ta.assignment_date = ? AND ta.status = 'scheduled'
            )
            ORDER BY d.full_name
        """;
        return jdbcTemplate.queryForList(sql, date);
    }

    public List<Map<String, Object>> getAvailableVehicles(LocalDate date) {
        String sql = """
            SELECT v.id, v.reg_number, vm.name as model_name, v.capacity
            FROM vehicle v
            JOIN vehicle_model vm ON v.model_id = vm.id
            WHERE v.status = 'active'
            AND NOT EXISTS (
                SELECT 1 FROM trip_assignment ta
                WHERE ta.vehicle_id = v.id AND ta.assignment_date = ? AND ta.status = 'scheduled'
            )
            ORDER BY v.reg_number
        """;
        return jdbcTemplate.queryForList(sql, date);
    }

    // === Просмотр рейсов ===

    public List<Map<String, Object>> getTripsByDate(LocalDate date) {
        String sql = """
        SELECT ta.id, ta.status, ta.actual_departure, ta.actual_arrival, ta.details,
               ta.driver_id, ta.vehicle_id,
               r.number as route_number, r.name as route_name,
               st.departure_time,
               (st.departure_time + (COALESCE(r.duration_min, 0) || ' minutes')::INTERVAL)::TIME AS planned_arrival,
               v.reg_number as vehicle_reg,
               d.full_name as driver_name,
               w.id as waybill_id,
               w.medical_ok, w.inspection_ok, w.ready as waybill_ready,
               w.mileage_before, w.mileage_after, w.fuel_before, w.fuel_after
        FROM trip_assignment ta
        LEFT JOIN route r ON ta.route_id = r.id
        LEFT JOIN schedule_template st ON ta.schedule_template_id = st.id
        LEFT JOIN vehicle v ON ta.vehicle_id = v.id
        LEFT JOIN driver d ON ta.driver_id = d.id
        LEFT JOIN waybill w ON w.trip_id = ta.id
        WHERE ta.assignment_date = ?
        ORDER BY st.departure_time
    """;
        return jdbcTemplate.queryForList(sql, date);
    }

    // === Путевой лист ===

    public List<Map<String, Object>> getWaybillsByDate(LocalDate date) {
        String sql = """
            SELECT w.id, w.trip_id, w.issue_date, w.status as waybill_status,
                   w.medical_ok, w.inspection_ok, w.ready,
                   w.mileage_before, w.mileage_after, w.fuel_before, w.fuel_after,
                   d.full_name as driver_name, v.reg_number as vehicle_reg,
                   r.number as route_number, r.name as route_name,
                   ta.status as trip_status
            FROM waybill w
            JOIN trip_assignment ta ON w.trip_id = ta.id
            JOIN driver d ON w.driver_id = d.id
            JOIN vehicle v ON w.vehicle_id = v.id
            LEFT JOIN route r ON ta.route_id = r.id
            WHERE w.issue_date = ?
            ORDER BY w.id
        """;
        return jdbcTemplate.queryForList(sql, date);
    }

    public Map<String, Object> getWaybillByTripId(Integer tripId) {
        String sql = """
        SELECT w.*, d.full_name as driver_name, v.reg_number as vehicle_reg
        FROM waybill w
        JOIN driver d ON w.driver_id = d.id
        JOIN vehicle v ON w.vehicle_id = v.id
        WHERE w.trip_id = ?
        ORDER BY w.id DESC
        LIMIT 1
    """;
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, tripId);
        return list.isEmpty() ? new HashMap<>() : list.get(0);
    }


    public List<Map<String, Object>> getMedicalByDate(LocalDate date) {
        String sql = """
        SELECT mc.*, d.full_name as driver_name,
        mc.check_date::text || ' ' || COALESCE(to_char(mc.created_at, 'HH24:MI'), '') as check_time
        FROM medical_check mc
        JOIN driver d ON mc.driver_id = d.id
        WHERE mc.check_date = ?
        ORDER BY mc.created_at DESC
    """;
        return jdbcTemplate.queryForList(sql, date);
    }

    public List<Map<String, Object>> getInspectionsByDate(LocalDate date) {
        String sql = """
        SELECT vi.*, v.reg_number as vehicle_reg, d.full_name as driver_name
        FROM vehicle_inspection vi
        LEFT JOIN vehicle v ON vi.vehicle_id = v.id
        LEFT JOIN driver d ON vi.driver_id = d.id
        WHERE vi.inspection_date = ?
        ORDER BY vi.inspection_date DESC
    """;
        return jdbcTemplate.queryForList(sql, date);
    }

    public List<Map<String, Object>> getFuelByDate(LocalDate date) {
        String sql = """
        SELECT fl.*, v.reg_number as vehicle_reg, d.full_name as driver_name
        FROM fuel_log fl
        LEFT JOIN vehicle v ON fl.vehicle_id = v.id
        LEFT JOIN driver d ON fl.driver_id = d.id
        WHERE fl.fuel_date = ?
        ORDER BY fl.fuel_date DESC
    """;
        return jdbcTemplate.queryForList(sql, date);
    }

    // === Проверка готовности ===

    public Map<String, Object> checkReadiness(Integer vehicleId, Integer driverId, LocalDate date) {
        String sql = """
            SELECT
                (SELECT COUNT(*) FROM medical_check WHERE driver_id = ? AND check_date = ? AND result = 'passed') > 0 AS med_ok,
                (SELECT COUNT(*) FROM vehicle_inspection WHERE vehicle_id = ? AND inspection_date = ? AND result = 'passed') > 0 AS insp_ok,
                (SELECT COUNT(*) FROM waybill WHERE driver_id = ? AND vehicle_id = ? AND issue_date = ?) > 0 AS waybill_ok,
                (SELECT COUNT(*) FROM waybill WHERE driver_id = ? AND vehicle_id = ? AND issue_date = ?
                    AND mileage_before IS NOT NULL) > 0 AS mileage_ok,
                (SELECT COUNT(*) FROM waybill WHERE driver_id = ? AND vehicle_id = ? AND issue_date = ?
                    AND fuel_before IS NOT NULL) > 0 AS fuel_ok
        """;
        return jdbcTemplate.queryForMap(sql,
                driverId, date, vehicleId, date,
                driverId, vehicleId, date,
                driverId, vehicleId, date,
                driverId, vehicleId, date);
    }


}