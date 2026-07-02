package com.example.JujutsuKaisen.DistributionMachines.Security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RepUser {

    private final JdbcTemplate jdbcTemplate;

    public RepUser(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AppUser> getAll() {
        String sql = """
        SELECT u.id, u.username, u.role_id, r.name as role_name, u.driver_id, u.enabled,
               d.full_name as driver_name
        FROM app_user u
        JOIN app_role r ON u.role_id = r.id
        LEFT JOIN driver d ON u.driver_id = d.id
        ORDER BY u.username
    """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AppUser u = new AppUser();
            u.setId(rs.getInt("id"));
            u.setUsername(rs.getString("username"));
            u.setRoleId(rs.getInt("role_id"));
            u.setRoleName(rs.getString("role_name"));
            u.setDriverId(rs.getInt("driver_id"));
            if (rs.getObject("driver_name") != null) {
                u.setDriverName(rs.getString("driver_name"));
            }
            u.setEnabled(rs.getBoolean("enabled"));
            return u;
        });
    }

    public void add(AppUser user) {
        String sql = "INSERT INTO app_user (username, password, role_id, driver_id) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, user.getUsername(), user.getPassword(), user.getRoleId(), user.getDriverId());
    }

    public void update(Integer id, AppUser user) {
        StringBuilder sql = new StringBuilder("UPDATE app_user SET ");
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();

        if (user.getUsername() != null) { sql.append("username = ?, "); params.add(user.getUsername()); }
        if (user.getPassword() != null) { sql.append("password = ?, "); params.add(user.getPassword()); }
        if (user.getRoleId() != null) { sql.append("role_id = ?, "); params.add(user.getRoleId()); }
        sql.append("driver_id = ?, "); params.add(user.getDriverId());

        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(id);

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", id);
    }
}