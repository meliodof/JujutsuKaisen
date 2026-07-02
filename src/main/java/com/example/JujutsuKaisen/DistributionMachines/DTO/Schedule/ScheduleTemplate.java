package com.example.JujutsuKaisen.DistributionMachines.DTO.Schedule;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
public class ScheduleTemplate {
    private Integer id;
    private Integer routeId;
    private Integer dayOfWeek;
    private LocalTime departureTime;
}