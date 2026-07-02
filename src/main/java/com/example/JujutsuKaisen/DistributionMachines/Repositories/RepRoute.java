package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import com.example.JujutsuKaisen.DistributionMachines.DTO.Route;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RepRoute {

    private final JdbcTemplate jdbcTemplate;

    public RepRoute(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // === Чтение ===

    public List<Route> getAll() {
        String sql = """
            SELECT r.id, r.number, r.name, r.length_km, r.duration_min, r.transport_type_id,
                   tt.name as transport_type_name
            FROM route r
            JOIN transport_type tt ON r.transport_type_id = tt.id
            ORDER BY r.number
        """;
        return jdbcTemplate.query(sql, this::mapRoute);
    }

    public Optional<Route> findById(Integer id) {
        String sql = """
            SELECT r.id, r.number, r.name, r.length_km, r.duration_min, r.transport_type_id,
                   tt.name as transport_type_name
            FROM route r
            JOIN transport_type tt ON r.transport_type_id = tt.id
            WHERE r.id = ?
        """;
        return jdbcTemplate.query(sql, this::mapRoute, id).stream().findFirst();
    }

    // === Запись ===

    public void add(Route route) {
        jdbcTemplate.update(
                "INSERT INTO route (number, name, length_km, duration_min, transport_type_id) VALUES (?, ?, ?, ?, ?)",
                route.getNumber(), route.getName(), route.getLengthKm(), route.getDurationMin(), route.getTransportTypeId()
        );
    }

    public void update(Integer id, Route route) {
        StringBuilder sql = new StringBuilder("UPDATE route SET ");
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        if (route.getNumber() != null) { sql.append("number = ?, "); params.add(route.getNumber()); }
        if (route.getName() != null) { sql.append("name = ?, "); params.add(route.getName()); }
        if (route.getLengthKm() != null) { sql.append("length_km = ?, "); params.add(route.getLengthKm()); }
        if (route.getDurationMin() != null) { sql.append("duration_min = ?, "); params.add(route.getDurationMin()); }
        if (route.getTransportTypeId() != null) { sql.append("transport_type_id = ?, "); params.add(route.getTransportTypeId()); }
        if (params.isEmpty()) return;
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(id);
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM route WHERE id = ?", id);
    }

    public void updateStats(Integer routeId) {
        jdbcTemplate.update("""
            UPDATE route SET
                length_km = (SELECT COALESCE(SUM(distance_to_next_km), 0) FROM route_stop_order WHERE route_id = ?),
                duration_min = (SELECT COALESCE(SUM(time_to_next_min), 0) FROM route_stop_order WHERE route_id = ?)
            WHERE id = ?
        """, routeId, routeId, routeId);
    }

    // === Проверки ===

    public boolean exists(Integer id) {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM route WHERE id = ?", Integer.class, id);
        return c != null && c > 0;
    }

    public boolean existsByNumber(String number) {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM route WHERE number = ?", Integer.class, number);
        return c != null && c > 0;
    }

    // === Маппинг ===

    private Route mapRoute(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Route r = new Route();
        r.setId(rs.getInt("id"));
        r.setNumber(rs.getString("number"));
        r.setName(rs.getString("name"));
        r.setLengthKm(rs.getDouble("length_km"));
        r.setDurationMin(rs.getInt("duration_min"));
        r.setTransportTypeId(rs.getInt("transport_type_id"));
        r.setTransportTypeName(rs.getString("transport_type_name"));
        return r;
    }
}