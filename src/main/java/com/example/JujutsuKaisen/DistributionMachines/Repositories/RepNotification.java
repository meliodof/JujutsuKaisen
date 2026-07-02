package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RepNotification {

    private final JdbcTemplate jdbcTemplate;

    public RepNotification(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer save(String type, String title, String message,
                        Integer tripId, Integer vehicleId, Integer driverId) {
        String sql = """
            INSERT INTO dispatcher_notification
                (notification_type, title, message, trip_id, vehicle_id, driver_id)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
        """;
        return jdbcTemplate.queryForObject(sql, Integer.class,
                type, title, message, tripId, vehicleId, driverId);
    }

    public Optional<Map<String, Object>> findById(Integer id) {
        String sql = """
            SELECT id, notification_type, title, message, trip_id, vehicle_id, driver_id,
                   is_read, created_at
            FROM dispatcher_notification WHERE id = ?
        """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<Map<String, Object>> findRecent(int limit, boolean unreadOnly) {
        String sql = unreadOnly
                ? """
            SELECT id, notification_type, title, message, trip_id, vehicle_id, driver_id,
                   is_read, created_at
            FROM dispatcher_notification
            WHERE is_read = FALSE
            ORDER BY created_at DESC
            LIMIT ?
        """
                : """
            SELECT id, notification_type, title, message, trip_id, vehicle_id, driver_id,
                   is_read, created_at
            FROM dispatcher_notification
            ORDER BY created_at DESC
            LIMIT ?
        """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    public List<Map<String, Object>> findSinceId(Integer sinceId, int limit) {
        if (sinceId == null || sinceId <= 0) {
            return findRecent(limit, false);
        }
        String sql = """
            SELECT id, notification_type, title, message, trip_id, vehicle_id, driver_id,
                   is_read, created_at
            FROM dispatcher_notification
            WHERE id > ?
            ORDER BY id ASC
            LIMIT ?
        """;
        return jdbcTemplate.queryForList(sql, sinceId, limit);
    }

    public int countUnread() {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatcher_notification WHERE is_read = FALSE", Integer.class);
        return c != null ? c : 0;
    }

    public void markRead(Integer id) {
        jdbcTemplate.update("UPDATE dispatcher_notification SET is_read = TRUE WHERE id = ?", id);
    }

    public void markAllRead() {
        jdbcTemplate.update("UPDATE dispatcher_notification SET is_read = TRUE WHERE is_read = FALSE");
    }
}
