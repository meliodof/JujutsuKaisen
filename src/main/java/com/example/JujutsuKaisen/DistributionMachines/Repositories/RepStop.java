package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Stop;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RepStop {

    private final JdbcTemplate jdbcTemplate;

    public RepStop(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // === Чтение ===

    public List<Stop> getAll() {
        return jdbcTemplate.query("SELECT id, name, latitude, longitude, stop_type FROM stop ORDER BY name", this::mapStop);
    }

    public Optional<Stop> findById(Integer id) {
        return jdbcTemplate.query("SELECT id, name, latitude, longitude, stop_type FROM stop WHERE id = ?", this::mapStop, id)
                .stream().findFirst();
    }

    // === Запись ===

    public void add(Stop stop) {
        jdbcTemplate.update(
                "INSERT INTO stop (name, latitude, longitude, stop_type) VALUES (?, ?, ?, ?)",
                stop.getName(), stop.getLatitude(), stop.getLongitude(), stop.getStopType()
        );
    }

    public void update(Integer id, Stop stop) {
        StringBuilder sql = new StringBuilder("UPDATE stop SET ");
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        if (stop.getName() != null) { sql.append("name = ?, "); params.add(stop.getName()); }
        if (stop.getLatitude() != null) { sql.append("latitude = ?, "); params.add(stop.getLatitude()); }
        if (stop.getLongitude() != null) { sql.append("longitude = ?, "); params.add(stop.getLongitude()); }
        if (stop.getStopType() != null) { sql.append("stop_type = ?, "); params.add(stop.getStopType()); }
        if (params.isEmpty()) return;
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(id);
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM stop WHERE id = ?", id);
    }

    // === Проверки ===

    public boolean exists(Integer id) {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stop WHERE id = ?", Integer.class, id);
        return c != null && c > 0;
    }

    public boolean existsByCoordinates(Double latitude, Double longitude) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stop WHERE latitude = ? AND longitude = ?", Integer.class, latitude, longitude);
        return c != null && c > 0;
    }

    public boolean existsByCoordinatesExcludingId(Double latitude, Double longitude, Integer id) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stop WHERE latitude = ? AND longitude = ? AND id != ?", Integer.class, latitude, longitude, id);
        return c != null && c > 0;
    }

    // === Маппинг ===

    private Stop mapStop(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Stop s = new Stop();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setLatitude(rs.getDouble("latitude"));
        if (rs.wasNull()) s.setLatitude(null);
        s.setLongitude(rs.getDouble("longitude"));
        if (rs.wasNull()) s.setLongitude(null);
        s.setStopType(rs.getString("stop_type"));
        return s;
    }
}