package com.example.JujutsuKaisen.DistributionMachines.Exceptions;

public class DriverBusyException extends RuntimeException {
    public DriverBusyException(Integer driverId, String date) {
        super("Водитель " + driverId + " уже занят на " + date);
    }
}