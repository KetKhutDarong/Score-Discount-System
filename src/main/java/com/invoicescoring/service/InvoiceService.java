package com.invoicescoring.service;

import com.invoicescoring.dto.InvoiceRequest;
import com.invoicescoring.model.Invoice;
import com.invoicescoring.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ScoreCalculationService scoreCalculationService;

    // Check if invoice number already exists
    public boolean invoiceNoExists(String invoiceNo) {
        return invoiceRepository.findByInvoiceNo(invoiceNo).isPresent();
    }

    // Create new invoice with MONTHLY score calculation
    @Transactional
    public Invoice createInvoice(InvoiceRequest request) {
        if (invoiceNoExists(request.getInvoiceNo())) {
            throw new RuntimeException("Invoice number already exists");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(request.getInvoiceNo());
        invoice.setDepotId(request.getDepotId());
        invoice.setDate(request.getDate());
        invoice.setPhone(request.getPhone());
        invoice.setAddress(request.getAddress());
        invoice.setAmount(request.getAmount());

        // TEMPORARY: Calculate individual invoice score
        Integer tempScore = scoreCalculationService.calculateScore(request.getAmount());
        Double tempDiscount = scoreCalculationService.calculateDiscount(tempScore);
        Long tempTotalAfterDiscount = scoreCalculationService.calculateTotalAfterDiscount(
                request.getAmount(), tempDiscount);

        invoice.setScore(tempScore);
        invoice.setDiscount(tempDiscount);
        invoice.setTotalAfterDiscount(tempTotalAfterDiscount);

        // Save the invoice first
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // RECALCULATE MONTHLY SCORES for this depot and month
        int month = request.getDate().getMonthValue();
        int year = request.getDate().getYear();
        recalculateMonthlyScores(request.getDepotId(), month, year);

        // Check if this is first order of calculation month (trigger calculation)
        checkAndTriggerQuarterCalculation(request.getDepotId(), request.getDate());

        return savedInvoice;
    }

    // Get all invoices for depot
    public List<Invoice> getDepotInvoices(String depotId) {
        return invoiceRepository.findByDepotIdOrderByDateDesc(depotId);
    }

    // Get invoice by ID
    public Optional<Invoice> getInvoiceById(String invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    // Update invoice with MONTHLY score recalculation
    @Transactional
    public Invoice updateInvoice(String invoiceId, InvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Store old month for recalculation
        int oldMonth = invoice.getDate().getMonthValue();
        int oldYear = invoice.getDate().getYear();

        invoice.setInvoiceNo(request.getInvoiceNo());
        invoice.setDate(request.getDate());
        invoice.setPhone(request.getPhone());
        invoice.setAddress(request.getAddress());
        invoice.setAmount(request.getAmount());

        // TEMPORARY: Calculate individual score
        Integer tempScore = scoreCalculationService.calculateScore(request.getAmount());
        Double tempDiscount = scoreCalculationService.calculateDiscount(tempScore);
        Long tempTotalAfterDiscount = scoreCalculationService.calculateTotalAfterDiscount(
                request.getAmount(), tempDiscount);

        invoice.setScore(tempScore);
        invoice.setDiscount(tempDiscount);
        invoice.setTotalAfterDiscount(tempTotalAfterDiscount);

        // Save the invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Recalculate scores for OLD month (if month changed)
        int newMonth = request.getDate().getMonthValue();
        int newYear = request.getDate().getYear();

        if (oldMonth != newMonth || oldYear != newYear) {
            recalculateMonthlyScores(invoice.getDepotId(), oldMonth, oldYear);
        }

        // Recalculate scores for NEW month
        recalculateMonthlyScores(invoice.getDepotId(), newMonth, newYear);

        return savedInvoice;
    }

    // Delete invoice with MONTHLY score recalculation
    @Transactional
    public void deleteInvoice(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        String depotId = invoice.getDepotId();
        int month = invoice.getDate().getMonthValue();
        int year = invoice.getDate().getYear();

        // Delete the invoice
        invoiceRepository.deleteById(invoiceId);

        // Recalculate monthly scores after deletion
        recalculateMonthlyScores(depotId, month, year);
    }

    // RECALCULATE MONTHLY SCORES for a specific month
    private void recalculateMonthlyScores(String depotId, int month, int year) {
        // Get all invoices for this depot in this specific month and year
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Invoice> monthInvoices = invoiceRepository.findByDepotIdAndDateBetween(
                depotId, startDate, endDate);

        if (!monthInvoices.isEmpty()) {
            // Calculate MONTHLY TOTAL amount
            long monthlyTotalAmount = 0;
            for (Invoice inv : monthInvoices) {
                monthlyTotalAmount += inv.getAmount();
            }

            // Calculate MONTHLY SCORE based on total amount
            Integer monthlyScore = scoreCalculationService.calculateAccumulatedScore(monthlyTotalAmount);
            Double monthlyDiscount = scoreCalculationService.calculateDiscount(monthlyScore);
            Long monthlyTotalAfterDiscount = scoreCalculationService.calculateTotalAfterDiscount(
                    monthlyTotalAmount, monthlyDiscount);

            // Distribute scores proportionally to invoices
            for (Invoice inv : monthInvoices) {
                // Calculate this invoice's share of the score (proportional)
                double percentage = (double) inv.getAmount() / monthlyTotalAmount;
                int invoiceScore = (int) Math.round(monthlyScore * percentage);

                // Ensure at least 1 score for invoices over 1,000,000
                if (inv.getAmount() >= 1_000_000L) {
                    int directScore = (int) (inv.getAmount() / 1_000_000L);
                    invoiceScore = Math.max(invoiceScore, directScore);
                }

                // Calculate invoice's total after discount
                Long invoiceTotalAfterDiscount = scoreCalculationService.calculateTotalAfterDiscount(
                        inv.getAmount(), monthlyDiscount);

                // Update invoice
                inv.setScore(invoiceScore);
                inv.setDiscount(monthlyDiscount);
                inv.setTotalAfterDiscount(invoiceTotalAfterDiscount);

                invoiceRepository.save(inv);
            }
        }
    }

    // Get invoices for specific month and year
    public List<Invoice> getInvoicesForMonth(String depotId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return invoiceRepository.findByDepotIdAndDateBetween(depotId, startDate, endDate);
    }

    // Get monthly total amount
    public Long getMonthlyTotalAmount(String depotId, int month, int year) {
        List<Invoice> monthInvoices = getInvoicesForMonth(depotId, month, year);
        return monthInvoices.stream()
                .mapToLong(Invoice::getAmount)
                .sum();
    }

    // Get monthly total score
    public Integer getMonthlyTotalScore(String depotId, int month, int year) {
        Long monthlyTotal = getMonthlyTotalAmount(depotId, month, year);
        return scoreCalculationService.calculateAccumulatedScore(monthlyTotal);
    }

    // Get invoice count for specific month
    public long getMonthInvoiceCount(String depotId, int month, int year) {
        List<Invoice> monthInvoices = getInvoicesForMonth(depotId, month, year);
        return monthInvoices.size();
    }

    // Check if calculation should be triggered
    private void checkAndTriggerQuarterCalculation(String depotId, LocalDate currentDate) {
        int month = currentDate.getMonthValue();
        int year = currentDate.getYear();

        // Only trigger in calculation months: April(4), July(7), October(10),
        // January(1)
        if (month == 4 || month == 7 || month == 10 || month == 1) {
            // Check if this is the first invoice of the calculation month
            List<Invoice> monthInvoices = getInvoicesForMonth(depotId, month, year);

            // If this is the first invoice of the calculation month
            if (monthInvoices.size() == 1) { // Current invoice + 0 existing = first invoice
                YearMonth quarterToCalculate;

                if (month == 4) {
                    quarterToCalculate = YearMonth.of(year, 1); // Q1: Jan-Mar
                } else if (month == 7) {
                    quarterToCalculate = YearMonth.of(year, 4); // Q2: Apr-Jun
                } else if (month == 10) {
                    quarterToCalculate = YearMonth.of(year, 7); // Q3: Jul-Sep
                } else { // month == 1 (January)
                    quarterToCalculate = YearMonth.of(year - 1, 10); // Q4: Oct-Dec of previous year
                }

                // Perform the calculation
                scoreCalculationService.performQuarterCalculation(depotId, quarterToCalculate);
            }
        }
    }

    // Get invoices by date range for depot
    public List<Invoice> getDepotInvoicesByDateRange(String depotId, LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByDepotIdAndDateBetween(depotId, startDate, endDate);
    }

    // Get total invoices for a depot
    public long getDepotInvoiceCount(String depotId) {
        return invoiceRepository.countByDepotId(depotId);
    }

    // Get invoices for a specific quarter
    public List<Invoice> getQuarterInvoices(String depotId, YearMonth quarterStart) {
        LocalDate startDate = quarterStart.atDay(1);
        LocalDate endDate = quarterStart.plusMonths(3).atDay(1).minusDays(1);
        return invoiceRepository.findByDepotIdAndDateBetweenOrderByDateAsc(depotId, startDate, endDate);
    }

    // Calculate quarterly totals
    public QuarterlyStats calculateQuarterlyStats(String depotId, YearMonth quarterStart) {
        List<Invoice> quarterInvoices = getQuarterInvoices(depotId, quarterStart);

        long totalAmount = 0;
        int invoiceCount = quarterInvoices.size();

        for (Invoice invoice : quarterInvoices) {
            totalAmount += invoice.getAmount();
        }

        QuarterlyStats stats = new QuarterlyStats();
        stats.setQuarter(quarterStart);
        stats.setTotalAmount(totalAmount);
        stats.setInvoiceCount(invoiceCount);
        stats.setTotalScore(scoreCalculationService.calculateAccumulatedScore(totalAmount));

        return stats;
    }

    // Inner class for quarterly stats
    public static class QuarterlyStats {
        private YearMonth quarter;
        private long totalAmount;
        private int invoiceCount;
        private int totalScore;

        public YearMonth getQuarter() {
            return quarter;
        }

        public void setQuarter(YearMonth quarter) {
            this.quarter = quarter;
        }

        public long getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(long totalAmount) {
            this.totalAmount = totalAmount;
        }

        public int getInvoiceCount() {
            return invoiceCount;
        }

        public void setInvoiceCount(int invoiceCount) {
            this.invoiceCount = invoiceCount;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(int totalScore) {
            this.totalScore = totalScore;
        }
    }
}