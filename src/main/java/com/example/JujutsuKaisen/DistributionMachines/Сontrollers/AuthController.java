package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import com.example.JujutsuKaisen.DistributionMachines.DTO.LoginRequest;
import com.example.JujutsuKaisen.DistributionMachines.DTO.LoginResponse;
import com.example.JujutsuKaisen.DistributionMachines.Security.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request.getUsername(), request.getPassword()));
    }
}