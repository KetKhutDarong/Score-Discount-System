package com.invoicescoring.service;

import com.invoicescoring.model.Admin;
import com.invoicescoring.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Initialize default admin user
    public void initializeDefaultAdmin() {
        try {
            Optional<Admin> existingAdmin = adminRepository.findByUsername("admin");
            if (existingAdmin.isEmpty()) {
                Admin admin = new Admin();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                adminRepository.save(admin);
                System.out.println("Default admin user created successfully");
            } else {
                System.out.println("Admin user already exists");
            }
        } catch (Exception e) {
            System.err.println("[v0] Error initializing admin user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Validate login
    public Optional<Admin> validateLogin(String username, String password) {
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            boolean passwordMatch = passwordEncoder.matches(password, admin.get().getPassword());
            if (passwordMatch) {
                return admin;
            }
        }
        return Optional.empty();
    }
}
