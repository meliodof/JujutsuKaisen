package com.example.JujutsuKaisen.DistributionMachines.Exceptions;

public class DuplicateException extends RuntimeException {
    public DuplicateException(String message) {
        super(message);
    }
}