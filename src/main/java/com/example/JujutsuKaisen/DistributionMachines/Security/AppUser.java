package com.example.JujutsuKaisen.DistributionMachines.Security;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppUser {
    private Integer id;
    private String username;
    private String password;
    private Integer roleId;
    private String roleName;
    private Integer driverId;
    private Boolean enabled;
    private String driverName;

}