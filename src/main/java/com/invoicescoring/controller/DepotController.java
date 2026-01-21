package com.invoicescoring.controller;

import com.invoicescoring.dto.DepotRequest;
import com.invoicescoring.model.Depot;
import com.invoicescoring.service.DepotService;
import com.invoicescoring.service.ScoreCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/depots")
@CrossOrigin(origins = "*")
public class DepotController {

    @Autowired
    private DepotService depotService;

    @Autowired
    private ScoreCalculationService scoreCalculationService;

    @PostMapping
    public Depot createDepot(@RequestBody DepotRequest request) {
        return depotService.createDepot(request);
    }

    @GetMapping
    public List<Depot> getAllDepots() {
        return depotService.getAllDepots();
    }

    @GetMapping("/{id}")
    public Depot getDepot(@PathVariable String id) {
        return depotService.getDepotById(id)
                .orElseThrow(() -> new RuntimeException("Depot not found"));
    }

    @PutMapping("/{id}")
    public Depot updateDepot(@PathVariable String id, @RequestBody DepotRequest request) {
        return depotService.updateDepot(id, request);
    }

    // NEW: Endpoint to update only remaining score
    @PatchMapping("/{id}/remaining-score")
    public Depot updateRemainingScore(@PathVariable String id, @RequestBody Map<String, Integer> request) {
        Integer remainingScore = request.get("remainingScore");
        return depotService.updateRemainingScore(id, remainingScore);
    }

    @DeleteMapping("/{id}")
    public void deleteDepot(@PathVariable String id) {
        depotService.deleteDepot(id);
    }

    @GetMapping("/{id}/carryover")
    public Map<String, Object> getCarryoverScore(@PathVariable String id) {
        // Get the latest calculation record for carryover
        YearMonth currentQuarter = YearMonth.now();
        YearMonth previousQuarter = currentQuarter.minusMonths(3);

        Integer carryover = scoreCalculationService.getCarryoverScore(id, currentQuarter);

        Map<String, Object> response = new HashMap<>();
        response.put("depotId", id);
        response.put("currentQuarter", currentQuarter.toString());
        response.put("previousQuarter", previousQuarter.toString());
        response.put("carryoverScore", carryover != null ? carryover : 0);

        return response;
    }

    // NEW: Get remaining score endpoint
    @GetMapping("/{id}/remaining-score")
    public Map<String, Object> getRemainingScore(@PathVariable String id) {
        Depot depot = depotService.getDepotById(id)
                .orElseThrow(() -> new RuntimeException("Depot not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("depotId", id);
        response.put("depotName", depot.getName());
        response.put("remainingScore", depot.getRemainingScore() != null ? depot.getRemainingScore() : 0);
        response.put("lastUpdated", depot.getUpdatedDate());

        return response;
    }
}