package com.invoicescoring.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "depots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Depot {
    @Id
    private String id;
    private String name;
    private String phone;
    private String address;
    private String additionalInfo;
    private Integer remainingScore = 0; // NEW: Added remaining score field
    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime updatedDate = LocalDateTime.now();
}