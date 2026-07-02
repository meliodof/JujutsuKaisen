package com.example.JujutsuKaisen.DistributionMachines.Сontrollers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // === Общие ===

    @GetMapping("/")                  public String index() { return "redirect:/login"; }
    @GetMapping("/login")             public String loginPage() { return "login"; }

    // === Админ ===

    @GetMapping("/admin/dashboard")   public String adminDashboard() { return "admin/dashboard"; }
    @GetMapping("/admin/dispatch")    public String adminDispatch() { return "admin/dispatch"; }
    @GetMapping("/admin/vehicles")    public String adminVehicles() { return "admin/vehicles"; }
    @GetMapping("/admin/drivers")     public String adminDrivers() { return "admin/drivers"; }
    @GetMapping("/admin/routes")      public String adminRoutes() { return "admin/routes"; }
    @GetMapping("/admin/schedules")   public String adminSchedules() { return "admin/schedules"; }
    @GetMapping("/admin/stops")       public String adminStops() { return "admin/stops"; }
    @GetMapping("/admin/users")       public String adminUsers() { return "admin/users"; }
    @GetMapping("/admin/logs")        public String adminLogs() { return "admin/logs"; }

    // === Диспетчер ===

    @GetMapping("/dispatcher/panel")      public String dispatcherPanel() { return "dispatcher/panel"; }
    @GetMapping("/dispatcher/dashboard") public String dispatcherDashboard() { return "dispatcher/dashboard"; }
    @GetMapping("/dispatcher/dispatch")  public String dispatcherDispatch() { return "dispatcher/dispatch"; }

    // === Водитель ===

    @GetMapping("/driver/dashboard")  public String driverDashboard() { return "driver/dashboard"; }

    // === Медик ===

    @GetMapping("/medical/panel")     public String medicalPanel() { return "medical/panel"; }

    // === Механик ===

    @GetMapping("/mechanic/panel")    public String mechanicPanel() { return "mechanic/panel"; }
}