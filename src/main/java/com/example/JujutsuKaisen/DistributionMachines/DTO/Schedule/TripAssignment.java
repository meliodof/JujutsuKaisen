package com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class TripAssignment {
    private Integer id;
    private Integer routeId;
    private Integer vehicleId;
    private Integer driverId;
    private Integer scheduleTemplateId;
    private LocalDate assignmentDate;
    private String status;
    private LocalDateTime actualDeparture;
    private LocalDateTime actualArrival;

    // Для daily schedule (JOIN)
    private String routeNumber;
    private LocalTime departureTime;
    private String vehicleReg;
    private String driverName;
}