package com.invoicescoring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboardPage(HttpSession session) {
        String adminId = (String) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        return "dashboard";
    }

    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/login";
    }

    // Update this to match your requirement
    @GetMapping("/depots/{depotId}") // PLURAL: depots
    public String depotDetailPage(@PathVariable String depotId, HttpSession session) {
        String adminId = (String) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        return "depot-detail"; // Make sure this matches your HTML file name
    }
}