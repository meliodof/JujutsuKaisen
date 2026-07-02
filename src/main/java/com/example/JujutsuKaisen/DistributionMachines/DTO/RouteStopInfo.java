package com.example.JujutsuKaisen.DistributionMachines.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteStopInfo {
    // Из route_stop_order
    private Integer routeId;
    private Integer orderNum;
    private Integer stopId;
    private Double distanceToNextKm;
    private Integer timeToNextMin;

    // Из stop (JOIN)
    private String stopName;
    private Double latitude;
    private Double longitude;
    private String stopType;
}