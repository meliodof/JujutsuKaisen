package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import com.example.JujutsuKaisen.DistributionMachines.DTO.RouteStopInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RepRouteStop {

    private final JdbcTemplate jdbcTemplate;

    public RepRouteStop(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // === Чтение ===

    public List<RouteStopInfo> getStopsByRouteId(Integer routeId) {
        String sql = """
            SELECT rso.route_id, rso.order_num, rso.stop_id, rso.distance_to_next_km, rso.time_to_next_min,
                   s.name as stop_name, s.latitude, s.longitude, s.stop_type
            FROM route_stop_order rso
            JOIN stop s ON rso.stop_id = s.id
            WHERE rso.route_id = ?
            ORDER BY rso.order_num
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            RouteStopInfo info = new RouteStopInfo();
            info.setRouteId(rs.getInt("route_id"));
            info.setOrderNum(rs.getInt("order_num"));
            info.setStopId(rs.getInt("stop_id"));
            info.setDistanceToNextKm(rs.getDouble("distance_to_next_km"));
            if (rs.wasNull()) info.setDistanceToNextKm(null);
            info.setTimeToNextMin(rs.getInt("time_to_next_min"));
            if (rs.wasNull()) info.setTimeToNextMin(null);
            info.setStopName(rs.getString("stop_name"));
            info.setLatitude(rs.getDouble("latitude"));
            if (rs.wasNull()) info.setLatitude(null);
            info.setLongitude(rs.getDouble("longitude"));
            if (rs.wasNull()) info.setLongitude(null);
            info.setStopType(rs.getString("stop_type"));
            return info;
        }, routeId);
    }

    // === Запись ===

    public void addStopToRoute(Integer routeId, Integer stopId, Integer orderNum, Double distanceToNextKm, Integer timeToNextMin) {
        jdbcTemplate.update(
                "INSERT INTO route_stop_order (route_id, order_num, stop_id, distance_to_next_km, time_to_next_min) VALUES (?, ?, ?, ?, ?)",
                routeId, orderNum, stopId, distanceToNextKm, timeToNextMin
        );
    }

    public void updateStopOnRoute(Integer routeId, Integer stopId, Integer orderNum, Double distanceToNextKm, Integer timeToNextMin) {
        jdbcTemplate.update(
                "UPDATE route_stop_order SET order_num = ?, distance_to_next_km = ?, time_to_next_min = ? WHERE route_id = ? AND stop_id = ?",
                orderNum, distanceToNextKm, timeToNextMin, routeId, stopId
        );
    }

    public void removeStopFromRoute(Integer routeId, Integer stopId) {
        jdbcTemplate.update("DELETE FROM route_stop_order WHERE route_id = ? AND stop_id = ?", routeId, stopId);
    }

    public void reorderStops(Integer routeId) {
        jdbcTemplate.update("""
            UPDATE route_stop_order rso SET order_num = tmp.new_order
            FROM (
                SELECT stop_id, ROW_NUMBER() OVER (ORDER BY order_num, stop_id) as new_order
                FROM route_stop_order WHERE route_id = ?
            ) tmp
            WHERE rso.route_id = ? AND rso.stop_id = tmp.stop_id
        """, routeId, routeId);
    }

    public void moveStop(Integer routeId, Integer stopId, int direction) {
        Integer currentOrder = jdbcTemplate.queryForObject(
                "SELECT order_num FROM route_stop_order WHERE route_id = ? AND stop_id = ?",
                Integer.class, routeId, stopId);
        if (currentOrder == null) return;

        int newOrder = currentOrder + direction;
        if (newOrder < 1) return;

        jdbcTemplate.update("UPDATE route_stop_order SET order_num = ? WHERE route_id = ? AND stop_id = ?",
                -currentOrder, routeId, stopId);
        jdbcTemplate.update("UPDATE route_stop_order SET order_num = ? WHERE route_id = ? AND order_num = ?",
                currentOrder, routeId, newOrder);
        jdbcTemplate.update("UPDATE route_stop_order SET order_num = ? WHERE route_id = ? AND stop_id = ?",
                newOrder, routeId, stopId);
    }

    // === Проверки ===

    public boolean existsByStopIdAndRouteId(Integer stopId, Integer routeId) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM route_stop_order WHERE stop_id = ? AND route_id = ?", Integer.class, stopId, routeId);
        return c != null && c > 0;
    }

    public boolean existsByOrderNum(Integer routeId, Integer orderNum) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM route_stop_order WHERE route_id = ? AND order_num = ?", Integer.class, routeId, orderNum);
        return c != null && c > 0;
    }
}