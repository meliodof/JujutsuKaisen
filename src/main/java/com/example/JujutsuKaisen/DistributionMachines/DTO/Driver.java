package com.example.JujutsuKaisen.DistributionMachines.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Driver {
    private Integer id;
    private String fullName;
    private Integer experienceYears;
    private String licenseCategory;
    private String phone;
    private String passportSeries;
    private String passportNumber;
    private String passportIssued;
    private LocalDate passportDate;
    private String licenseNumber;
    private LocalDate licenseDate;
    private String photoPath;
}