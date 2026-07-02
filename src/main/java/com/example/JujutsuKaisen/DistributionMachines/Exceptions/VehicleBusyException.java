package com.example.JujutsuKaisen.DistributionMachines.Exceptions;

public class VehicleBusyException extends RuntimeException {
    public VehicleBusyException(Integer vehicleId, String date) {
        super("Машина " + vehicleId + " уже занята на " + date);
    }
}