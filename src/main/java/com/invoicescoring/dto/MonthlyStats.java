package com.invoicescoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyStats {
    private int month;
    private long totalAmount;
    private long invoiceCount;
    private int totalScore;
}