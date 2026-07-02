package com.example.JujutsuKaisen.DistributionMachines.Security;

import com.example.JujutsuKaisen.DistributionMachines.EntityType;
import com.example.JujutsuKaisen.DistributionMachines.DTO.LoginResponse;
import com.example.JujutsuKaisen.DistributionMachines.Services.LogService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final RepAuth repository;
    private final LogService logService;
    private final PasswordService passwordService;

    public AuthService(RepAuth repository, LogService logService, PasswordService passwordService) {
        this.repository = repository;
        this.logService = logService;
        this.passwordService = passwordService;
    }

    public LoginResponse login(String username, String password) {
        try {
            // Получаем хеш из БД
            Map<String, Object> result = repository.login(username);

            String encodedPassword = (String) result.get("out_password_hash");
            if (!passwordService.matches(password, encodedPassword)) {
                logService.log("LOGIN_FAILED", String.valueOf(EntityType.USER), null, username, "Неверный пароль");
                throw new RuntimeException("Неверный пароль");
            }

            LoginResponse response = new LoginResponse();
            response.setUsername((String) result.get("out_username"));
            response.setRole((String) result.get("out_role_name"));
            response.setDriverId((Integer) result.get("out_driver_id"));
            response.setToken("jwt-token-" + username);
            logService.log("LOGIN", String.valueOf(EntityType.USER), null, username, "role=" + response.getRole());
            return response;

        } catch (Exception e) {
            logService.log("LOGIN_FAILED", String.valueOf(EntityType.USER), null, username, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}