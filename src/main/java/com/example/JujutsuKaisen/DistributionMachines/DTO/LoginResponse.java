package com.example.JujutsuKaisen.DistributionMachines.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private String role;
    private String username;
    private Integer driverId;
}

