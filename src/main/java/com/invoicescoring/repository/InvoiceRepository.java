package com.invoicescoring.repository;

import com.invoicescoring.model.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Optional<Invoice> findByInvoiceNo(String invoiceNo);
    List<Invoice> findByDepotIdOrderByDateDesc(String depotId);
    List<Invoice> findByDepotIdAndDateBetween(String depotId, LocalDate startDate, LocalDate endDate);
    List<Invoice> findByDepotIdAndDateBetweenOrderByDateAsc(String depotId, LocalDate startDate, LocalDate endDate);
    long countByDepotId(String depotId);
}
