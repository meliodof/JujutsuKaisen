package com.example.JujutsuKaisen.DistributionMachines.DTO.Vehicle;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Vehicle {
    private Integer id;
    private String regNumber;
    private Integer modelId;
    private Integer capacity;
    private String status;
    private String transportTypeName;

    // Для отображения (JOIN)
    private String modelName;
    private String fuelTypeName;
    private Integer defaultCapacity;
}