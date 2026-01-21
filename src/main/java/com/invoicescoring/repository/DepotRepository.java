package com.invoicescoring.repository;

import com.invoicescoring.model.Depot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepotRepository extends MongoRepository<Depot, String> {
    List<Depot> findAllByOrderByCreatedDateDesc();
}
