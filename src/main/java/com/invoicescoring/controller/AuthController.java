package com.invoicescoring.controller;

import com.invoicescoring.dto.AuthResponse;
import com.invoicescoring.dto.LoginRequest;
import com.invoicescoring.model.Admin;
import com.invoicescoring.service.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.OPTIONS })
public class AuthController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpSession session) {
        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Username: " + request.getUsername());
        System.out.println("Session ID before login: " + session.getId());

        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(
                    new AuthResponse(false, "Username and password are required", null));
        }

        Optional<Admin> admin = adminService.validateLogin(request.getUsername(), request.getPassword());

        if (admin.isPresent()) {
            session.setAttribute("adminId", admin.get().getId());
            session.setAttribute("username", admin.get().getUsername());

            System.out.println("Login SUCCESS!");
            System.out.println("Session ID after login: " + session.getId());
            System.out.println("adminId in session: " + session.getAttribute("adminId"));
            System.out.println("username in session: " + session.getAttribute("username"));

            return ResponseEntity.ok(
                    new AuthResponse(true, "Login successful", admin.get().getId()));
        }

        System.out.println("Login FAILED - Invalid credentials");
        return ResponseEntity.status(401).body(
                new AuthResponse(false, "Invalid username or password", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(
                new AuthResponse(true, "Logout successful", null));
    }

    @GetMapping("/check")
    public ResponseEntity<AuthResponse> checkAuth(HttpSession session) {
        System.out.println("=== CHECK AUTH ===");
        System.out.println("Session ID: " + session.getId());
        System.out.println("adminId in session: " + session.getAttribute("adminId"));

        String adminId = (String) session.getAttribute("adminId");
        if (adminId != null) {
            return ResponseEntity.ok(
                    new AuthResponse(true, "Authenticated", adminId));
        }
        return ResponseEntity.status(401).body(
                new AuthResponse(false, "Not authenticated", null));
    }

    // Add this debug endpoint
    @GetMapping("/debug")
    public String debug(HttpSession session) {
        return "Session ID: " + session.getId() +
                "<br>Admin ID: " + session.getAttribute("adminId") +
                "<br>Username: " + session.getAttribute("username");
    }
}