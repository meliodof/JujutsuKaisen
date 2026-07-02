package com.example.JujutsuKaisen.DistributionMachines.Exceptions;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entity, Integer id) {
        super(entity + " с id " + id + " не найден(а)");
    }
}