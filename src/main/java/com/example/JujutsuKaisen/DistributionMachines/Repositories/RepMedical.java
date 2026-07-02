package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class RepMedical {

    private final JdbcTemplate jdbcTemplate;

    public RepMedical(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getByDate(LocalDate date) {
        String sql = """
            SELECT mc.*, d.full_name as driver_name
            FROM medical_check mc JOIN driver d ON mc.driver_id = d.id
            WHERE mc.check_date = ? ORDER BY mc.check_date DESC
        """;
        return jdbcTemplate.queryForList(sql, date);
    }

    public void save(Integer driverId, String checkType, String result, String notes, String checkedBy) {
        String sql = "INSERT INTO medical_check (driver_id, check_type, result, notes, checked_by, check_date) VALUES (?, ?, ?, ?, ?, CURRENT_DATE)";
        jdbcTemplate.update(sql, driverId, checkType, result, notes, checkedBy);
    }

    public void approveWaybill(Integer driverId) {
        jdbcTemplate.update("UPDATE waybill SET medical_ok = true WHERE driver_id = ? AND issue_date = CURRENT_DATE", driverId);
    }

    public void updateReadiness(Integer driverId) {
        jdbcTemplate.update("UPDATE waybill SET ready = (medical_ok AND inspection_ok) WHERE driver_id = ? AND issue_date = CURRENT_DATE", driverId);
    }

    public boolean existsToday(Integer driverId, String checkType) {
        String sql = "SELECT COUNT(*) FROM medical_check WHERE driver_id = ? AND check_date = CURRENT_DATE AND check_type = ?";
        Integer c = jdbcTemplate.queryForObject(sql, Integer.class, driverId, checkType);
        return c != null && c > 0;
    }
}