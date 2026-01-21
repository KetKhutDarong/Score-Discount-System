package com.invoicescoring.controller;

import com.invoicescoring.dto.DashboardStats;
import com.invoicescoring.repository.InvoiceRepository;
import com.invoicescoring.service.DepotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DepotService depotService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping("/stats")
    public DashboardStats getStats() {
        long totalDepots = depotService.getDepotCount();
        long totalInvoices = invoiceRepository.count();
        Long totalAmount = invoiceRepository.findAll()
                .stream()
                .mapToLong(invoice -> invoice.getAmount() != null ? invoice.getAmount() : 0)
                .sum();

        return new DashboardStats(totalDepots, totalInvoices, totalAmount);
    }
}
