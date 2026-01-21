package com.invoicescoring.service;

import com.invoicescoring.model.CalculationRecord;
import com.invoicescoring.model.Invoice;
import com.invoicescoring.repository.CalculationRecordRepository;
import com.invoicescoring.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class ScoreCalculationService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CalculationRecordRepository calculationRecordRepository;

    // Calculate score for SINGLE invoice - This is WRONG for monthly accumulation
    public Integer calculateScore(Long amount) {
        if (amount < 1_000_000)
            return 0;
        return Math.toIntExact(amount / 1_000_000);
    }

    // NEW: Calculate score for accumulated amount
    public Integer calculateAccumulatedScore(Long totalAmount) {
        if (totalAmount < 1_000_000)
            return 0;
        return Math.toIntExact(totalAmount / 1_000_000);
    }

    // NEW: Calculate score for a list of invoices (monthly accumulation)
    public Integer calculateMonthlyScore(List<Invoice> invoices) {
        long totalAmount = 0;
        for (Invoice invoice : invoices) {
            totalAmount += invoice.getAmount();
        }
        return calculateAccumulatedScore(totalAmount);
    }

    // Calculate discount based on TOTAL score (not individual)
    public Double calculateDiscount(Integer totalScore) {
        if (totalScore >= 20)
            return 10.0;
        if (totalScore >= 10)
            return 5.0;
        return 0.0;
    }

    // Calculate total after discount
    public Long calculateTotalAfterDiscount(Long amount, Double discount) {
        return Math.round(amount * (1 - discount / 100.0));
    }

    // Perform 3-month accumulation calculation
    public void performQuarterCalculation(String depotId, YearMonth quarterStart) {
        LocalDate startDate = quarterStart.atDay(1);
        LocalDate endDate = quarterStart.plusMonths(3).atDay(1).minusDays(1);

        // Sum all invoices in the 3-month period
        List<Invoice> invoices = invoiceRepository.findByDepotIdAndDateBetweenOrderByDateAsc(
                depotId, startDate, endDate);

        long totalAmount = 0;
        Integer totalScore = 0;

        for (Invoice invoice : invoices) {
            totalAmount += invoice.getAmount();
            // DON'T add individual scores - calculate from total amount
            // totalScore += invoice.getScore(); // WRONG
        }

        // CORRECT: Calculate score from TOTAL amount
        totalScore = calculateAccumulatedScore(totalAmount);

        // Calculate discount
        Double discount = calculateDiscount(totalScore);
        Long totalAfterDiscount = calculateTotalAfterDiscount(totalAmount, discount);

        // Calculate remainder score
        Integer remainderScore = totalScore % 10;

        // Calculate new score from total after discount
        Integer newScore = calculateAccumulatedScore(totalAfterDiscount);
        Integer carryoverScore = remainderScore + newScore;

        // Save calculation record
        CalculationRecord record = new CalculationRecord();
        record.setDepotId(depotId);
        record.setQuarter(quarterStart);
        record.setTotalAmount(totalAmount);
        record.setTotalScore(totalScore);
        record.setDiscount(discount);
        record.setTotalAfterDiscount(totalAfterDiscount);
        record.setRemainderScore(remainderScore);
        record.setCarryoverScore(carryoverScore);
        record.setCalculatedDate(LocalDate.now());
        calculationRecordRepository.save(record);

        // Delete invoices from previous 3 months
        invoiceRepository.deleteAll(invoices);
    }

    // Get carried over score for depot in a specific quarter
    public Integer getCarryoverScore(String depotId, YearMonth quarter) {
        YearMonth previousQuarter = quarter.minusMonths(3);
        return calculationRecordRepository.findByDepotIdAndQuarter(depotId, previousQuarter)
                .map(CalculationRecord::getCarryoverScore)
                .orElse(0);
    }
}