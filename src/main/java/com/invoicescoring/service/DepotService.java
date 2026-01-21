package com.invoicescoring.service;

import com.invoicescoring.dto.DepotRequest;
import com.invoicescoring.model.Depot;
import com.invoicescoring.repository.DepotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DepotService {

    @Autowired
    private DepotRepository depotRepository;

    // Create new depot
    public Depot createDepot(DepotRequest request) {
        Depot depot = new Depot();
        depot.setName(request.getName());
        depot.setPhone(request.getPhone());
        depot.setAddress(request.getAddress());
        depot.setAdditionalInfo(request.getAdditionalInfo());
        depot.setRemainingScore(request.getRemainingScore() != null ? request.getRemainingScore() : 0); // NEW
        depot.setCreatedDate(LocalDateTime.now());
        return depotRepository.save(depot);
    }

    // Get all depots
    public List<Depot> getAllDepots() {
        return depotRepository.findAllByOrderByCreatedDateDesc();
    }

    // Get depot by ID
    public Optional<Depot> getDepotById(String depotId) {
        return depotRepository.findById(depotId);
    }

    // Update depot
    public Depot updateDepot(String depotId, DepotRequest request) {
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new RuntimeException("Depot not found"));

        depot.setName(request.getName());
        depot.setPhone(request.getPhone());
        depot.setAddress(request.getAddress());
        depot.setAdditionalInfo(request.getAdditionalInfo());
        depot.setRemainingScore(request.getRemainingScore() != null ? request.getRemainingScore() : 0); // NEW
        depot.setUpdatedDate(LocalDateTime.now());

        return depotRepository.save(depot);
    }

    // Update only remaining score
    public Depot updateRemainingScore(String depotId, Integer remainingScore) {
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new RuntimeException("Depot not found"));

        depot.setRemainingScore(remainingScore != null ? remainingScore : 0);
        depot.setUpdatedDate(LocalDateTime.now());

        return depotRepository.save(depot);
    }

    // Delete depot
    public void deleteDepot(String depotId) {
        depotRepository.deleteById(depotId);
    }

    // Get depot count
    public long getDepotCount() {
        return depotRepository.count();
    }
}