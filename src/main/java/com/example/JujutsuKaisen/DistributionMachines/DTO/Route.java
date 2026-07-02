package com.example.JujutsuKaisen.DistributionMachines.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Route {
    private Integer id;
    private String number;
    private String name;
    private Double lengthKm;
    private Integer durationMin;
    private Integer transportTypeId;
    private String transportTypeName;
}