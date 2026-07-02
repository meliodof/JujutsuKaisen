package com.example.JujutsuKaisen.DistributionMachines.DTO.Vehicle;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleModel {
    private Integer id;
    private String name;
    private Integer fuelTypeId;
    private Integer defaultCapacity;

    // Для отображения (JOIN)
    private String fuelTypeName;
}