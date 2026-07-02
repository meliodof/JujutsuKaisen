package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.ScheduleTemplate;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.TripAssignment;
import com.example.JujutsuKaisen.DistributionMachines.Exceptions.DuplicateException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Repository
public class RepSchedule {

    private final JdbcTemplate jdbcTemplate;

    public RepSchedule(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // === Расписание ===

    public List<ScheduleTemplate> getScheduleByRoute(Integer routeId, Integer dayOfWeek) {
        return jdbcTemplate.query("SELECT * FROM schedule_get_by_route(?, ?)", (rs, rowNum) -> {
            ScheduleTemplate st = new ScheduleTemplate();
            st.setId(rs.getInt("id"));
            st.setRouteId(rs.getInt("route_id"));
            st.setDayOfWeek(rs.getInt("day_of_week"));
            st.setDepartureTime(rs.getTime("departure_time").toLocalTime());
            return st;
        }, routeId, dayOfWeek);
    }

    public List<Map<String, Object>> getAllTemplates() {
        String sql = """
            SELECT st.id, st.route_id, st.day_of_week, st.departure_time,
                   r.number as route_number, r.name as route_name,
                   (st.departure_time + (COALESCE(r.duration_min, 0) || ' minutes')::INTERVAL)::TIME AS planned_arrival
            FROM schedule_template st
            JOIN route r ON st.route_id = r.id
            ORDER BY st.day_of_week, st.departure_time
        """;
        return jdbcTemplate.queryForList(sql);
    }

    public void addTemplate(ScheduleTemplate st) {
        jdbcTemplate.update(
                "INSERT INTO schedule_template (route_id, day_of_week, departure_time) VALUES (?, ?, ?)",
                st.getRouteId(), st.getDayOfWeek(), st.getDepartureTime()
        );
    }

    public void deleteTemplate(Integer id) {
        jdbcTemplate.update("DELETE FROM schedule_template WHERE id = ?", id);
    }

    public boolean existsTemplate(Integer routeId, Integer dayOfWeek, LocalTime departureTime) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schedule_template WHERE route_id = ? AND day_of_week = ? AND departure_time = ?",
                Integer.class, routeId, dayOfWeek, departureTime
        );
        return c != null && c > 0;
    }

    public List<TripAssignment> getDailySchedule(LocalDate date) {
        String sql = "SELECT * FROM trip_get_daily_schedule(?)";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TripAssignment t = new TripAssignment();
            t.setRouteNumber(rs.getString("route_number"));
            t.setDepartureTime(rs.getTime("departure_time").toLocalTime());
            t.setVehicleReg(rs.getString("vehicle_reg"));
            t.setDriverName(rs.getString("driver_name"));
            t.setStatus(rs.getString("status"));
            return t;
        }, date);
    }

    // === Рейсы ===

    public void assignTrip(TripAssignment trip) {
        jdbcTemplate.update("CALL trip_assign_safe(?, ?, ?, ?, ?, ?)",
                trip.getRouteId(), trip.getVehicleId(), trip.getDriverId(),
                trip.getScheduleTemplateId(), trip.getAssignmentDate(),
                trip.getStatus() != null ? trip.getStatus() : "scheduled"
        );
    }

    public void updateStatus(Integer id, String status, LocalDateTime dep, LocalDateTime arr,
                             String details, Integer startMileage, Integer endMileage) {
        String sql = "UPDATE trip_assignment SET status = ?, actual_departure = COALESCE(?, actual_departure), actual_arrival = COALESCE(?, actual_arrival), details = COALESCE(?, details), start_mileage = COALESCE(?, start_mileage), end_mileage = COALESCE(?, end_mileage) WHERE id = ?";
        jdbcTemplate.update(sql, status, dep, arr, details, startMileage, endMileage, id);
    }

    // === Путевой лист ===

    public void createWaybill(Integer tripId) {
        String checkSql = "SELECT COUNT(*) FROM waybill WHERE trip_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tripId);
        if (count != null && count > 0) {
            throw new DuplicateException("Путевой лист уже создан для этого рейса");
        }

        String sql = """
        INSERT INTO waybill (trip_id, driver_id, vehicle_id, issue_date, status)
        SELECT ta.id, ta.driver_id, ta.vehicle_id, CURRENT_DATE, 'issued'
        FROM trip_assignment ta WHERE ta.id = ?
    """;
        jdbcTemplate.update(sql, tripId);
    }

    public boolean existsWaybill(Integer tripId) {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM waybill WHERE trip_id = ?", Integer.class, tripId);
        return c != null && c > 0;
    }

    public Map<String, Object> getTripContext(Integer tripId) {
        String sql = """
            SELECT ta.id, ta.driver_id, ta.vehicle_id, ta.status, ta.details,
                   r.number AS route_number, r.name AS route_name,
                   d.full_name AS driver_name, v.reg_number AS vehicle_reg
            FROM trip_assignment ta
            LEFT JOIN route r ON ta.route_id = r.id
            LEFT JOIN driver d ON ta.driver_id = d.id
            LEFT JOIN vehicle v ON ta.vehicle_id = v.id
            WHERE ta.id = ?
        """;
        return jdbcTemplate.queryForMap(sql, tripId);
    }

    public Map<String, Object> checkReadiness(Integer vehicleId, Integer driverId, LocalDate date) {
        String sql = """
            SELECT 
                (SELECT COUNT(*) FROM medical_check WHERE driver_id = ? AND check_date = ? AND result = 'passed') > 0 AS med_ok,
                (SELECT COUNT(*) FROM vehicle_inspection WHERE vehicle_id = ? AND inspection_date = ? AND result = 'passed') > 0 AS insp_ok,
                (SELECT COUNT(*) FROM waybill WHERE driver_id = ? AND vehicle_id = ? AND issue_date = ?) > 0 AS waybill_ok
        """;
        return jdbcTemplate.queryForMap(sql, driverId, date, vehicleId, date, driverId, vehicleId, date);
    }

}