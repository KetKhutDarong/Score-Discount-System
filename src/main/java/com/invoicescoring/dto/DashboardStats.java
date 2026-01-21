package com.invoicescoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStats {
    private long totalDepots;
    private long totalInvoices;
    private Long totalAmount;
}
