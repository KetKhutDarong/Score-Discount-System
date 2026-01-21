package com.invoicescoring.controller;

import com.invoicescoring.dto.InvoiceRequest;
import com.invoicescoring.dto.MonthlyStats;
import com.invoicescoring.model.Invoice;
import com.invoicescoring.service.InvoiceService;
import com.invoicescoring.service.ScoreCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ScoreCalculationService scoreCalculationService;

    @PostMapping
    public Invoice createInvoice(@RequestBody InvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @GetMapping("/depot/{depotId}")
    public List<Invoice> getDepotInvoices(@PathVariable String depotId) {
        return invoiceService.getDepotInvoices(depotId);
    }

    @GetMapping("/{id}")
    public Invoice getInvoice(@PathVariable String id) {
        return invoiceService.getInvoiceById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    @PutMapping("/{id}")
    public Invoice updateInvoice(@PathVariable String id, @RequestBody InvoiceRequest request) {
        return invoiceService.updateInvoice(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteInvoice(@PathVariable String id) {
        invoiceService.deleteInvoice(id);
    }

    @GetMapping("/export/{id}")
    public ResponseEntity<String> exportInvoice(@PathVariable String id) throws IOException {
        Invoice invoice = invoiceService.getInvoiceById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        StringBuilder content = new StringBuilder();
        content.append("Invoice No: ").append(invoice.getInvoiceNo()).append("\n");
        content.append("Depot: ").append(invoice.getDepotName()).append("\n");
        content.append("Date: ").append(invoice.getDate()).append("\n");
        content.append("Amount: ").append(invoice.getAmount()).append(" ៛\n");
        content.append("Score: ").append(invoice.getScore()).append("\n");
        content.append("Discount: ").append(invoice.getDiscount()).append("%\n");
        content.append("Total After Discount: ").append(invoice.getTotalAfterDiscount()).append(" ៛");

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + invoice.getInvoiceNo() + ".txt\"")
                .header("Content-Type", "text/plain; charset=utf-8")
                .body(content.toString());
    }

    @GetMapping("/depot/{depotId}/monthly")
    public List<MonthlyStats> getMonthlyStats(@PathVariable String depotId) {
        List<Invoice> allInvoices = invoiceService.getDepotInvoices(depotId);

        Map<Integer, MonthlyStats> monthlyMap = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyMap.put(month, new MonthlyStats(month, 0L, 0L, 0));
        }

        for (Invoice invoice : allInvoices) {
            int month = invoice.getDate().getMonthValue();
            MonthlyStats stats = monthlyMap.get(month);
            stats.setTotalAmount(stats.getTotalAmount() + invoice.getAmount());
            stats.setInvoiceCount(stats.getInvoiceCount() + 1);
            stats.setTotalScore(stats.getTotalScore() + invoice.getScore());
        }

        return new ArrayList<>(monthlyMap.values());
    }

    @GetMapping("/depot/{depotId}/year-stats")
    public Map<String, Object> getYearStats(@PathVariable String depotId) {
        List<Invoice> allInvoices = invoiceService.getDepotInvoices(depotId);

        int currentYear = LocalDate.now().getYear();
        long totalYearAmount = 0;
        int totalYearScore = 0;
        int totalInvoices = 0;

        for (Invoice invoice : allInvoices) {
            if (invoice.getDate().getYear() == currentYear) {
                totalYearAmount += invoice.getAmount();
                totalYearScore += invoice.getScore();
                totalInvoices++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalYearAmount", totalYearAmount);
        stats.put("totalYearScore", totalYearScore);
        stats.put("totalInvoices", totalInvoices);
        stats.put("year", currentYear);

        return stats;
    }

    // Get invoices for specific month and year
    @GetMapping("/depot/{depotId}/month/{month}")
    public List<Invoice> getMonthInvoices(@PathVariable String depotId, @PathVariable int month) {
        List<Invoice> allInvoices = invoiceService.getDepotInvoices(depotId);
        int currentYear = LocalDate.now().getYear();

        List<Invoice> monthInvoices = new ArrayList<>();
        for (Invoice invoice : allInvoices) {
            LocalDate invoiceDate = invoice.getDate();
            if (invoiceDate.getMonthValue() == month && invoiceDate.getYear() == currentYear) {
                monthInvoices.add(invoice);
            }
        }

        return monthInvoices;
    }

    // Get invoice count for specific month
    @GetMapping("/depot/{depotId}/month/{month}/count")
    public Map<String, Object> getMonthInvoiceCount(@PathVariable String depotId, @PathVariable int month) {
        List<Invoice> monthInvoices = getMonthInvoices(depotId, month);

        Map<String, Object> response = new HashMap<>();
        response.put("depotId", depotId);
        response.put("month", month);
        response.put("count", monthInvoices.size());
        response.put("year", LocalDate.now().getYear());

        return response;
    }

    // Get invoices by date range
    @GetMapping("/depot/{depotId}/date-range")
    public List<Invoice> getInvoicesByDateRange(
            @PathVariable String depotId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        return invoiceService.getDepotInvoicesByDateRange(depotId, start, end);
    }

    // Get quarter invoices (3 months accumulation)
    @GetMapping("/depot/{depotId}/quarter")
    public Map<String, Object> getQuarterInvoices(@PathVariable String depotId, @RequestParam int startMonth) {
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate, endDate;

        // Determine quarter based on start month
        if (startMonth == 1) { // Q1: Jan-Mar
            startDate = LocalDate.of(currentYear, 1, 1);
            endDate = LocalDate.of(currentYear, 3, 31);
        } else if (startMonth == 4) { // Q2: Apr-Jun
            startDate = LocalDate.of(currentYear, 4, 1);
            endDate = LocalDate.of(currentYear, 6, 30);
        } else if (startMonth == 7) { // Q3: Jul-Sep
            startDate = LocalDate.of(currentYear, 7, 1);
            endDate = LocalDate.of(currentYear, 9, 30);
        } else { // Q4: Oct-Dec
            startDate = LocalDate.of(currentYear, 10, 1);
            endDate = LocalDate.of(currentYear, 12, 31);
        }

        List<Invoice> quarterInvoices = invoiceService.getDepotInvoicesByDateRange(depotId, startDate, endDate);

        long totalAmount = 0;
        int totalScore = 0;

        for (Invoice invoice : quarterInvoices) {
            totalAmount += invoice.getAmount();
            totalScore += invoice.getScore();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("quarterStartMonth", startMonth);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("totalInvoices", quarterInvoices.size());
        response.put("totalAmount", totalAmount);
        response.put("totalScore", totalScore);
        response.put("invoices", quarterInvoices);

        return response;
    }

    // Get carryover score for depot
    @GetMapping("/depot/{depotId}/carryover")
    public Map<String, Object> getCarryoverScore(@PathVariable String depotId) {
        YearMonth currentQuarter = YearMonth.now();
        YearMonth previousQuarter = currentQuarter.minusMonths(3);

        Integer carryover = scoreCalculationService.getCarryoverScore(depotId, currentQuarter);

        Map<String, Object> response = new HashMap<>();
        response.put("depotId", depotId);
        response.put("currentQuarter", currentQuarter.toString());
        response.put("previousQuarter", previousQuarter.toString());
        response.put("carryoverScore", carryover != null ? carryover : 0);

        return response;
    }

    // Check if quarter has been calculated
    @GetMapping("/depot/{depotId}/calculated")
    public Map<String, Object> checkQuarterCalculated(
            @PathVariable String depotId,
            @RequestParam int month,
            @RequestParam int year) {

        YearMonth quarterToCheck;

        // Determine which quarter this month belongs to
        if (month >= 1 && month <= 3) {
            quarterToCheck = YearMonth.of(year, 1); // Q1
        } else if (month >= 4 && month <= 6) {
            quarterToCheck = YearMonth.of(year, 4); // Q2
        } else if (month >= 7 && month <= 9) {
            quarterToCheck = YearMonth.of(year, 7); // Q3
        } else {
            quarterToCheck = YearMonth.of(year, 10); // Q4
        }

        // This would check if calculation record exists
        // For now, return a simple response
        Map<String, Object> response = new HashMap<>();
        response.put("depotId", depotId);
        response.put("quarter", quarterToCheck.toString());
        response.put("calculated", false); // You'll need to implement actual check
        response.put("calculationMonth", getCalculationMonthForQuarter(quarterToCheck.getMonthValue()));

        return response;
    }

    // Trigger manual calculation for a quarter
    @PostMapping("/depot/{depotId}/calculate-quarter")
    public Map<String, Object> calculateQuarter(
            @PathVariable String depotId,
            @RequestParam int quarterMonth) {

        YearMonth quarter;
        int year = LocalDate.now().getYear();

        if (quarterMonth == 1) {
            quarter = YearMonth.of(year, 1); // Q1
        } else if (quarterMonth == 4) {
            quarter = YearMonth.of(year, 4); // Q2
        } else if (quarterMonth == 7) {
            quarter = YearMonth.of(year, 7); // Q3
        } else {
            quarter = YearMonth.of(year, 10); // Q4
        }

        try {
            scoreCalculationService.performQuarterCalculation(depotId, quarter);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Quarter calculation completed successfully");
            response.put("quarter", quarter.toString());
            response.put("depotId", depotId);

            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Calculation failed: " + e.getMessage());

            return response;
        }
    }

    // Helper method to get calculation month for a quarter
    private int getCalculationMonthForQuarter(int quarterStartMonth) {
        if (quarterStartMonth == 1)
            return 4; // Q1 calculated in April
        if (quarterStartMonth == 4)
            return 7; // Q2 calculated in July
        if (quarterStartMonth == 7)
            return 10; // Q3 calculated in October
        return 1; // Q4 calculated in January next year
    }

    // Get all invoices grouped by month for current year
    @GetMapping("/depot/{depotId}/grouped-by-month")
    public Map<Integer, List<Invoice>> getInvoicesGroupedByMonth(@PathVariable String depotId) {
        List<Invoice> allInvoices = invoiceService.getDepotInvoices(depotId);
        int currentYear = LocalDate.now().getYear();

        Map<Integer, List<Invoice>> grouped = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            grouped.put(month, new ArrayList<>());
        }

        for (Invoice invoice : allInvoices) {
            LocalDate invoiceDate = invoice.getDate();
            if (invoiceDate.getYear() == currentYear) {
                int month = invoiceDate.getMonthValue();
                grouped.get(month).add(invoice);
            }
        }

        return grouped;
    }

    // Validate invoice number uniqueness
    @GetMapping("/check-invoice-no/{invoiceNo}")
    public Map<String, Object> checkInvoiceNo(@PathVariable String invoiceNo) {
        boolean exists = invoiceService.invoiceNoExists(invoiceNo);

        Map<String, Object> response = new HashMap<>();
        response.put("invoiceNo", invoiceNo);
        response.put("exists", exists);
        response.put("message", exists ? "Invoice number already exists" : "Invoice number available");

        return response;
    }
}