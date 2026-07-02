package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.Security.AppUser;
import com.example.JujutsuKaisen.DistributionMachines.Security.PasswordService;
import com.example.JujutsuKaisen.DistributionMachines.Security.RepUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final RepUser repository;
    private final PasswordService passwordService;

    public UserService(RepUser repository, PasswordService passwordService) {
        this.repository = repository;
        this.passwordService = passwordService;
    }

    @Transactional(readOnly = true)
    public List<AppUser> getAll() {
        return repository.getAll();
    }

    @Transactional
    public void add(AppUser user) {
        repository.add(user);
    }

    @Transactional
    public void update(Integer id, AppUser user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordService.encode(user.getPassword()));
        }
        repository.update(id, user);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(id);
    }
}