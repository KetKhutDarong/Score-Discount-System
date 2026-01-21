package com.invoicescoring.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class InvoiceRequest {
    private String invoiceNo;
    private String depotId;
    private LocalDate date;
    private String phone;
    private String address;
    private Long amount;
}
