package com.example.JujutsuKaisen.DistributionMachines.Repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class RepLog {

    private final JdbcTemplate jdbcTemplate;

    public RepLog(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String operationType, String entityType, Integer entityId,
                     String username, String details) {
        String sql = "INSERT INTO operation_log (operation_type, entity_type, entity_id, username, details) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, operationType, entityType, entityId, username, details);
    }

    public List<Map<String, Object>> getLogs(int limit) {
        String sql = """
            SELECT ol.id, ol.operation_type, et.name as entity_name, ol.entity_id, 
                   ol.username, ol.details, ol.created_at
            FROM operation_log ol
            JOIN entity_type et ON ol.entity_type = et.code
            ORDER BY ol.created_at DESC
            LIMIT ?
        """;
        return jdbcTemplate.queryForList(sql, limit);
    }
}