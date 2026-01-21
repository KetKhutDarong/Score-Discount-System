package com.invoicescoring.repository;

import com.invoicescoring.model.CalculationRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.YearMonth;
import java.util.Optional;

public interface CalculationRecordRepository extends MongoRepository<CalculationRecord, String> {
    Optional<CalculationRecord> findByDepotIdAndQuarter(String depotId, YearMonth quarter);
}