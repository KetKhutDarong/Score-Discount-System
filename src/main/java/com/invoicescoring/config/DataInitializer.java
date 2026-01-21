package com.invoicescoring.config;

import com.invoicescoring.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminService adminService;

    @Override
    public void run(String... args) throws Exception {
        // Initialize default admin user on startup
        adminService.initializeDefaultAdmin();
    }
}
