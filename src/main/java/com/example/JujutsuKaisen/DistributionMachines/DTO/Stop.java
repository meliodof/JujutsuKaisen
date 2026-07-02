package com.example.JujutsuKaisen.DistributionMachines.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Stop {
    private Integer id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String stopType;
    private Integer routeId;
    private Integer orderNum;
    private Double distanceToNextKm;
    private Integer timeToNextMin;
}