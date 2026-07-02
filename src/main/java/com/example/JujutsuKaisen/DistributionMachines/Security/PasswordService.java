package com.example.JujutsuKaisen.DistributionMachines.Security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Хешировать пароль
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // Проверить пароль
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}