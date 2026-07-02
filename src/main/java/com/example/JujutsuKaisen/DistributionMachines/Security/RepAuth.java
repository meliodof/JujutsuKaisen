package com.example.JujutsuKaisen.DistributionMachines.Security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public class RepAuth {

    private final JdbcTemplate jdbcTemplate;

    public RepAuth(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> login(String username) {
        String sql = "SELECT * FROM user_login(?)";
        return jdbcTemplate.queryForMap(sql, username);
    }
}