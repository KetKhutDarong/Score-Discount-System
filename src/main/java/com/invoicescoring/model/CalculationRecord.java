package com.invoicescoring.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.YearMonth;

@Document(collection = "calculation_records")
@Data
public class CalculationRecord {
    @Id
    private String id;
    private String depotId;
    private YearMonth quarter;
    private long totalAmount;
    private int totalScore;
    private double discount;
    private long totalAfterDiscount;
    private int remainderScore;
    private int carryoverScore;
    private LocalDate calculatedDate;
}