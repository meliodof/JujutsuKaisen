package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepLog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LogService {

    private final RepLog repository;

    public LogService(RepLog repository) {
        this.repository = repository;
    }

    public void log(String operationType, String entityType, Integer entityId,
                    String username, String details) {
        repository.save(operationType, entityType, entityId, username, details);
    }

    public List<Map<String, Object>> getLogs(int limit) {
        return repository.getLogs(limit);
    }
}