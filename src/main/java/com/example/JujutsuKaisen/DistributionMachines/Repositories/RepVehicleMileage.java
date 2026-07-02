package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class RepVehicleMileage {

    private final JdbcTemplate jdbcTemplate;

    public RepVehicleMileage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer getLastMileage(Integer vehicleId) {
        String sql = """
            SELECT GREATEST(
                COALESCE((SELECT MAX(mileage) FROM fuel_log WHERE vehicle_id = ?), 0),
                COALESCE((SELECT MAX(mileage_after) FROM waybill WHERE vehicle_id = ?), 0),
                COALESCE((SELECT MAX(mileage_before) FROM waybill WHERE vehicle_id = ?), 0),
                COALESCE((SELECT MAX(end_mileage) FROM trip_assignment WHERE vehicle_id = ?), 0),
                COALESCE((SELECT MAX(start_mileage) FROM trip_assignment WHERE vehicle_id = ?), 0)
            ) AS last_mileage
        """;
        return jdbcTemplate.queryForObject(sql, Integer.class,
                vehicleId, vehicleId, vehicleId, vehicleId, vehicleId);
    }

    public void syncWaybillMileageFromTrip(Integer tripId, Integer startMileage, Integer endMileage) {
        if (startMileage != null) {
            jdbcTemplate.update(
                    "UPDATE waybill SET mileage_before = COALESCE(mileage_before, ?) WHERE trip_id = ?",
                    startMileage, tripId);
        }
        if (endMileage != null) {
            jdbcTemplate.update(
                    "UPDATE waybill SET mileage_after = ? WHERE trip_id = ? AND mileage_before IS NOT NULL",
                    endMileage, tripId);
        }
    }

    public Integer getTripVehicleId(Integer tripId) {
        return jdbcTemplate.queryForObject(
                "SELECT vehicle_id FROM trip_assignment WHERE id = ?", Integer.class, tripId);
    }

    public Map<String, Object> getTripMileageFields(Integer tripId) {
        return jdbcTemplate.queryForMap(
                "SELECT vehicle_id, start_mileage, end_mileage FROM trip_assignment WHERE id = ?", tripId);
    }

    public List<Map<String, Object>> getVehicleControlByDate(LocalDate date) {
        String sql = """
            SELECT v.id AS vehicle_id, v.reg_number,
                   ta.id AS trip_id, ta.status AS trip_status,
                   d.full_name AS driver_name,
                   w.id AS waybill_id,
                   w.mileage_before, w.mileage_after,
                   w.fuel_before, w.fuel_after,
                   ta.start_mileage, ta.end_mileage,
                   CASE
                       WHEN w.id IS NULL THEN 'Нет путевого листа'
                       WHEN w.mileage_before IS NULL THEN 'Нет пробега до выезда'
                       WHEN ta.status IN ('completed') AND w.mileage_after IS NULL THEN 'Нет пробега после рейса'
                       WHEN w.mileage_after IS NOT NULL AND w.mileage_before IS NOT NULL
                            AND w.mileage_after < w.mileage_before THEN 'Пробег после меньше пробега до'
                       WHEN w.fuel_before IS NULL THEN 'Нет топлива до выезда'
                       WHEN ta.status IN ('completed') AND w.fuel_after IS NULL THEN 'Нет топлива после рейса'
                       ELSE NULL
                   END AS alert
            FROM trip_assignment ta
            JOIN vehicle v ON ta.vehicle_id = v.id
            LEFT JOIN driver d ON ta.driver_id = d.id
            LEFT JOIN waybill w ON w.trip_id = ta.id
            WHERE ta.assignment_date = ?
            ORDER BY v.reg_number, ta.id
        """;
        return jdbcTemplate.queryForList(sql, date);
    }
}
