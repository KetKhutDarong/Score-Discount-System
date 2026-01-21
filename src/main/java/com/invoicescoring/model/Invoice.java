package com.invoicescoring.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    private String id;
    private String invoiceNo;
    private String depotId;
    private String depotName;
    private LocalDate date;
    private String phone;
    private String address;
    private Long amount; // in៛
    private Integer score; // auto calculated
    private Double discount; // auto calculated in %
    private Long totalAfterDiscount; // amount * (1 - discount/100)
    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime updatedDate = LocalDateTime.now();
}
