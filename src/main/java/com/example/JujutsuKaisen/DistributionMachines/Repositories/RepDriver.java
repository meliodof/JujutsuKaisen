package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Driver;
import com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule.TripAssignment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class RepDriver {

    private final JdbcTemplate jdbcTemplate;

    public RepDriver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // === Чтение ===

    public List<Driver> getAll() {
        String sql = "SELECT id, full_name, experience_years, license_category, phone, " +
                "passport_series, passport_number, passport_issued, passport_date, " +
                "license_number, license_date, photo_path FROM driver ORDER BY full_name";
        return jdbcTemplate.query(sql, this::mapDriver);
    }

    public Optional<Driver> findById(Integer id) {
        String sql = "SELECT id, full_name, experience_years, license_category, phone, " +
                "passport_series, passport_number, passport_issued, passport_date, " +
                "license_number, license_date, photo_path FROM driver WHERE id = ?";
        return jdbcTemplate.query(sql, this::mapDriver, id).stream().findFirst();
    }

    public List<Driver> getAvailable(LocalDate date) {
        String sql = """
            SELECT d.id, d.full_name, d.experience_years, d.license_category, d.phone,
                   d.passport_series, d.passport_number, d.passport_issued, d.passport_date,
                   d.license_number, d.license_date, d.photo_path
            FROM driver d
            WHERE NOT EXISTS (
                SELECT 1 FROM trip_assignment ta
                WHERE ta.driver_id = d.id AND ta.assignment_date = ? AND ta.status = 'scheduled'
            )
            ORDER BY d.full_name
        """;
        return jdbcTemplate.query(sql, this::mapDriver, date);
    }

    public List<TripAssignment> getTrips(Integer driverId, LocalDate date) {
        return jdbcTemplate.query("SELECT * FROM driver_get_trips(?, ?)", this::mapTrip, driverId, date);
    }

    public List<TripAssignment> getTodaySchedule(Integer driverId) {
        String sql = """
            SELECT ta.id, ta.route_id, ta.vehicle_id, ta.driver_id, ta.assignment_date,
                   ta.status, r.number as route_number, st.departure_time, v.reg_number as vehicle_reg
            FROM trip_assignment ta
            JOIN route r ON ta.route_id = r.id
            JOIN schedule_template st ON ta.schedule_template_id = st.id
            LEFT JOIN vehicle v ON ta.vehicle_id = v.id
            WHERE ta.driver_id = ? AND ta.assignment_date = ?
            ORDER BY st.departure_time
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TripAssignment t = new TripAssignment();
            t.setId(rs.getInt("id"));
            t.setRouteId(rs.getInt("route_id"));
            t.setVehicleId(rs.getInt("vehicle_id"));
            t.setDriverId(rs.getInt("driver_id"));
            t.setAssignmentDate(rs.getDate("assignment_date").toLocalDate());
            t.setStatus(rs.getString("status"));
            t.setRouteNumber(rs.getString("route_number"));
            t.setDepartureTime(rs.getTime("departure_time") != null ? rs.getTime("departure_time").toLocalTime() : null);
            t.setVehicleReg(rs.getString("vehicle_reg"));
            return t;
        }, driverId, LocalDate.now());
    }

    // === Запись ===

    public void add(Driver driver) {
        jdbcTemplate.update("CALL driver_add(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                driver.getFullName(), driver.getExperienceYears(), driver.getLicenseCategory(),
                driver.getPhone(), driver.getPassportSeries(), driver.getPassportNumber(),
                driver.getPassportIssued(), driver.getPassportDate(), driver.getLicenseNumber(),
                driver.getLicenseDate(), driver.getPhotoPath());
    }

    public void update(Integer id, Driver driver) {
        StringBuilder sql = new StringBuilder("UPDATE driver SET ");
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        if (driver.getFullName() != null) { sql.append("full_name = ?, "); params.add(driver.getFullName()); }
        if (driver.getExperienceYears() != null) { sql.append("experience_years = ?, "); params.add(driver.getExperienceYears()); }
        if (driver.getLicenseCategory() != null) { sql.append("license_category = ?, "); params.add(driver.getLicenseCategory()); }
        if (driver.getPhone() != null) { sql.append("phone = ?, "); params.add(driver.getPhone()); }
        if (driver.getPassportSeries() != null) { sql.append("passport_series = ?, "); params.add(driver.getPassportSeries()); }
        if (driver.getPassportNumber() != null) { sql.append("passport_number = ?, "); params.add(driver.getPassportNumber()); }
        if (driver.getPassportIssued() != null) { sql.append("passport_issued = ?, "); params.add(driver.getPassportIssued()); }
        if (driver.getPassportDate() != null) { sql.append("passport_date = ?, "); params.add(driver.getPassportDate()); }
        if (driver.getLicenseNumber() != null) { sql.append("license_number = ?, "); params.add(driver.getLicenseNumber()); }
        if (driver.getLicenseDate() != null) { sql.append("license_date = ?, "); params.add(driver.getLicenseDate()); }
        if (driver.getPhotoPath() != null) { sql.append("photo_path = ?, "); params.add(driver.getPhotoPath()); }
        if (params.isEmpty()) return;
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(id);
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM driver WHERE id = ?", id);
    }

    // === Проверки ===

    public boolean exists(Integer id) {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM driver WHERE id = ?", Integer.class, id);
        return c != null && c > 0;
    }

    public boolean existsByFullName(String s) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM driver WHERE full_name = ?", Integer.class, s) > 0;
    }

    public boolean existsByFullNameExcludingId(String s, Integer id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM driver WHERE full_name = ? AND id != ?", Integer.class, s, id) > 0;
    }

    public boolean existsByPhone(String s) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM driver WHERE phone = ?", Integer.class, s) > 0;
    }

    public boolean existsByPhoneExcludingId(String s, Integer id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM driver WHERE phone = ? AND id != ?", Integer.class, s, id) > 0;
    }

    public boolean existsByPassport(String series, String number) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM driver WHERE passport_series = ? AND passport_number = ?", Integer.class, series, number) > 0;
    }

    public boolean existsByPassportExcludingId(String series, String number, Integer id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM driver WHERE passport_series = ? AND passport_number = ? AND id != ?", Integer.class, series, number, id) > 0;
    }

    public boolean existsByLicenseNumber(String s) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM driver WHERE license_number = ?", Integer.class, s) > 0;
    }

    public boolean existsByLicenseNumberExcludingId(String s, Integer id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM driver WHERE license_number = ? AND id != ?", Integer.class, s, id) > 0;
    }

    // === Маппинг ===

    private Driver mapDriver(ResultSet rs, int rowNum) throws SQLException {
        Driver d = new Driver();
        d.setId(rs.getInt("id"));
        d.setFullName(rs.getString("full_name"));
        d.setExperienceYears(rs.getInt("experience_years"));
        d.setLicenseCategory(rs.getString("license_category"));
        d.setPhone(rs.getString("phone"));
        d.setPassportSeries(rs.getString("passport_series"));
        d.setPassportNumber(rs.getString("passport_number"));
        d.setPassportIssued(rs.getString("passport_issued"));
        if (rs.getDate("passport_date") != null) d.setPassportDate(rs.getDate("passport_date").toLocalDate());
        d.setLicenseNumber(rs.getString("license_number"));
        if (rs.getDate("license_date") != null) d.setLicenseDate(rs.getDate("license_date").toLocalDate());
        d.setPhotoPath(rs.getString("photo_path"));
        return d;
    }

    private TripAssignment mapTrip(ResultSet rs, int rowNum) throws SQLException {
        TripAssignment t = new TripAssignment();
        t.setId(rs.getInt("id"));
        t.setRouteId(rs.getInt("route_id"));
        t.setVehicleId(rs.getInt("vehicle_id"));
        t.setDriverId(rs.getInt("driver_id"));
        t.setAssignmentDate(rs.getDate("assignment_date").toLocalDate());
        t.setStatus(rs.getString("status"));
        return t;
    }
}