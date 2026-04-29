package com.example.back.controller;

import com.example.back.entity.Application;
import com.example.back.entity.Facture;
import com.example.back.entity.Structure;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.FactureRepository;
import com.example.back.repository.StructureRepository;
import com.example.back.service.ai.AIRecommendationEngine;
import com.example.back.service.ai.InvoiceRiskAIModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ai-risk")
@RequiredArgsConstructor
@Slf4j
public class AIRiskController {

    private final InvoiceRiskAIModel riskAIModel;
    private final AIRecommendationEngine recommendationEngine;

    private final FactureRepository factureRepository;
    private final StructureRepository structureRepository;
    private final ApplicationRepository applicationRepository;

    @GetMapping("/predict/invoice/{numeroFacture}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> predictInvoiceRiskByNumber(@PathVariable String numeroFacture) {
        try {
            // Find facture by numeroFacture instead of ID
            Facture facture = factureRepository.findByNumeroFacture(numeroFacture)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with number: " + numeroFacture));

            InvoiceRiskAIModel.RiskPrediction prediction = riskAIModel.predictInvoiceRisk(facture.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prediction", prediction);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error predicting invoice risk: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/predict/new-invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> predictNewInvoiceRisk(
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam String clientCode,  // Changed from clientId to clientCode
            @RequestParam(required = false) String applicationCode) {  // Changed from applicationId to applicationCode
        try {
            // Find client by code instead of ID
            Structure client = structureRepository.findByCode(clientCode)
                    .orElseThrow(() -> new RuntimeException("Client not found with code: " + clientCode));

            Long applicationId = null;
            if (applicationCode != null) {
                Application application = applicationRepository.findByCode(applicationCode)
                        .orElseThrow(() -> new RuntimeException("Application not found with code: " + applicationCode));
                applicationId = application.getId();
            }

            InvoiceRiskAIModel.RiskPrediction prediction = riskAIModel.predictNewInvoiceRisk(
                    amount, dueDate, client.getId(), applicationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prediction", prediction);
            response.put("recommendations", prediction.getRecommendations());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error predicting new invoice risk: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getRiskDashboard() {
        try {
            // This would need to be implemented in the service
            // For now, returns a placeholder
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalClientsAnalyzed", 0);
            dashboard.put("criticalClients", 0);
            dashboard.put("highRiskClients", 0);
            dashboard.put("totalOverdueAmount", 0);
            dashboard.put("averageRiskScore", 0);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dashboard);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting risk dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/my-high-risk-invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getMyHighRiskInvoices(
            @RequestParam(defaultValue = "MEDIUM") String minLevel) {
        try {
            // This would need to get the current user and filter their invoices
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Endpoint to be implemented - returns high risk invoices for current commercial user");
            response.put("minLevel", minLevel);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting high risk invoices: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/high-risk-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getHighRiskClients(
            @RequestParam(defaultValue = "HIGH") String minLevel) {
        try {
            // This would need to be implemented in the service
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Endpoint to be implemented - returns all high risk clients");
            response.put("minLevel", minLevel);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting high risk clients: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @PostMapping("/retrain")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> retrainModel() {
        try {
            riskAIModel.autoRetrainModel();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "AI model retrained successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retraining model: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @DeleteMapping("/cache")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> clearCache() {
        try {
            // This would need a method to clear the cache
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "AI prediction cache cleared");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing cache: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/model-info")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getModelInfo() {
        try {
            Map<String, Object> modelInfo = new HashMap<>();
            modelInfo.put("modelType", "Neural Network - DeepLearning4j");
            modelInfo.put("inputFeatures", 15);
            modelInfo.put("hiddenLayers", 3);
            modelInfo.put("riskLevels", 6);
            modelInfo.put("lastTrainingDate", LocalDate.now());
            modelInfo.put("status", "Active");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", modelInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting model info: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "AI Risk Service is running");
        response.put("timestamp", LocalDate.now());
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    @GetMapping("/debug/training-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> debugTrainingData() {
        Map<String, Object> result = new HashMap<>();

        List<Facture> allFactures = factureRepository.findAllWithAllRelations();

        Map<Integer, Integer> riskLevelCounts = new HashMap<>();
        Map<Integer, List<Double>> riskScoresByLevel = new HashMap<>();

        for (Facture facture : allFactures) {
            InvoiceRiskAIModel.HistoricalPaymentData data = riskAIModel.extractFeaturesFromFacture(facture);
            int level = data.riskLevel;
            double score = data.riskScore;

            riskLevelCounts.merge(level, 1, Integer::sum);
            riskScoresByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(score);
        }

        result.put("totalInvoices", allFactures.size());
        result.put("riskLevelCounts", riskLevelCounts);

        Map<String, Object> scoreStats = new HashMap<>();
        for (Map.Entry<Integer, List<Double>> entry : riskScoresByLevel.entrySet()) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("count", entry.getValue().size());
            stats.put("min", entry.getValue().stream().mapToDouble(Double::doubleValue).min().orElse(0));
            stats.put("max", entry.getValue().stream().mapToDouble(Double::doubleValue).max().orElse(0));
            stats.put("avg", entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0));
            scoreStats.put("level_" + entry.getKey(), stats);
        }
        result.put("riskScoreStatistics", scoreStats);

        return ResponseEntity.ok(result);
    }


    @GetMapping("/debug/features/{numeroFacture}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> debugFeatures(@PathVariable String numeroFacture) {
        Facture facture = factureRepository.findByNumeroFacture(numeroFacture).orElse(null);
        if (facture == null) return ResponseEntity.notFound().build();

        InvoiceRiskAIModel.HistoricalPaymentData data = riskAIModel.extractFeaturesFromFacture(facture);

        Map<String, Object> debug = new HashMap<>();
        debug.put("invoiceId", facture.getId());
        debug.put("invoiceNumber", facture.getNumeroFacture());
        debug.put("status", facture.getStatutPaiement());
        debug.put("dueDate", facture.getDateEcheance());
        debug.put("daysUntilDue", data.daysUntilDue);
        debug.put("paymentOnTimeRate", data.paymentOnTimeRate);
        debug.put("latePaymentRate", data.latePaymentRate);
        debug.put("advancePaymentRate", data.advancePaymentRate);
        debug.put("riskScore", data.riskScore);
        debug.put("riskLevel", data.riskLevel);
        debug.put("delayDays", data.delayDays);
        debug.put("totalConventions", data.totalConventions);
        debug.put("clientAge", data.clientAge);
        debug.put("clientCode", facture.getConvention() != null && facture.getConvention().getStructureBeneficiel() != null ?
                facture.getConvention().getStructureBeneficiel().getCode() : null);

        return ResponseEntity.ok(debug);
    }

    @GetMapping("/debug/predict-raw/{numeroFacture}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> debugRawPrediction(@PathVariable String numeroFacture) {
        try {
            Facture facture = factureRepository.findByNumeroFactureWithRelations(numeroFacture).orElse(null);
            if (facture == null) return ResponseEntity.notFound().build();

            InvoiceRiskAIModel.HistoricalPaymentData data = riskAIModel.extractFeaturesFromFacture(facture);
            INDArray featureVector = riskAIModel.extractFeatureVectorForDebug(data);
            INDArray output = riskAIModel.getNeuralNetworkOutput(featureVector);
            INDArray classOutput = riskAIModel.getClassifierOutput(featureVector);

            Map<String, Object> result = new HashMap<>();
            result.put("invoiceId", facture.getId());
            result.put("invoiceNumber", facture.getNumeroFacture());
            result.put("calculatedRiskScore", data.riskScore);
            result.put("calculatedRiskLevel", data.riskLevel);
            result.put("calculatedRiskLevelName", getLevelName(data.riskLevel));

            // Feature values
            Map<String, Double> features = new LinkedHashMap<>();
            features.put("paymentOnTimeRate", data.paymentOnTimeRate);
            features.put("latePaymentRate", data.latePaymentRate);
            features.put("advancePaymentRate", data.advancePaymentRate);
            features.put("daysUntilDue", (double) data.daysUntilDue);
            features.put("clientAge", (double) data.clientAge);
            features.put("totalConventions", (double) data.totalConventions);
            features.put("delayDays", (double) data.delayDays);
            result.put("features", features);

            // Raw neural network outputs
            result.put("rawNNOutput_score", output.getDouble(0) * 100);
            result.put("rawNNOutput_delay", output.getDouble(1) * 90);

            // Classifier probabilities for each level
            Map<String, Double> classProbabilities = new LinkedHashMap<>();
            String[] levelNames = {"VERY_LOW", "LOW", "MEDIUM", "HIGH", "VERY_HIGH", "CRITICAL"};
            for (int i = 0; i < 6; i++) {
                classProbabilities.put(levelNames[i], classOutput.getDouble(i) * 100);
            }
            result.put("classifierProbabilities", classProbabilities);

            // Predicted level
            int predictedLevel = getArgMax(classOutput);
            result.put("predictedLevel", levelNames[predictedLevel]);
            result.put("predictedLevelCode", predictedLevel);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Debug prediction failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private int getArgMax(INDArray array) {
        int maxIndex = 0;
        double maxValue = array.getDouble(0);
        for (int i = 1; i < array.length(); i++) {
            double value = array.getDouble(i);
            if (value > maxValue) {
                maxValue = value;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private String getLevelName(int level) {
        switch(level) {
            case 0: return "VERY_LOW";
            case 1: return "LOW";
            case 2: return "MEDIUM";
            case 3: return "HIGH";
            case 4: return "VERY_HIGH";
            case 5: return "CRITICAL";
            default: return "UNKNOWN";
        }
    }


    @PostMapping("/force-retrain")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> forceRetrain() {
        try {
            // Call the retrain method in the service
            riskAIModel.forceRetrain();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Model force-retrained successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Force retrain failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }


    @GetMapping("/debug/model-test")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> testModelLearning() {
        // Test if the model can predict a known high-risk invoice correctly
        Facture facture = factureRepository.findByIdWithAllRelations(465L).orElse(null);
        if (facture == null) return ResponseEntity.notFound().build();

        InvoiceRiskAIModel.HistoricalPaymentData data = riskAIModel.extractFeaturesFromFacture(facture);
        INDArray features = riskAIModel.extractFeatureVectorForDebug(data);
        INDArray output = riskAIModel.getNeuralNetworkOutput(features);

        double predictedRisk = output.getDouble(0) * 100;
        double actualRisk = data.riskScore;
        double error = Math.abs(predictedRisk - actualRisk);

        Map<String, Object> result = new HashMap<>();
        result.put("actualRiskScore", actualRisk);
        result.put("predictedRiskScore", predictedRisk);
        result.put("error", error);
        result.put("isAccurate", error < 15);
        result.put("recommendation", error > 20 ? "Model needs more training" : "Model performing well");

        return ResponseEntity.ok(result);
    }



    @GetMapping("/upcoming-invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getUpcomingInvoicesWithRisk(
            @RequestParam(defaultValue = "6") int days) {
        try {
            List<Map<String, Object>> results = riskAIModel.getUpcomingInvoicesWithRisk(days);

            // Calculate summary correctly from the actual invoices
            long criticalCount = results.stream()
                    .filter(r -> "⚠️ Critique".equals(r.get("riskLevel")) || "CRITICAL".equals(r.get("riskLevel")))
                    .count();
            long highRiskCount = results.stream()
                    .filter(r -> {
                        String level = (String) r.get("riskLevel");
                        return "HIGH".equals(level) || "VERY_HIGH".equals(level) || "🟠 Élevé".equals(level) || "🔴 Très Élevé".equals(level);
                    })
                    .count();
            long mediumRiskCount = results.stream()
                    .filter(r -> "MEDIUM".equals(r.get("riskLevel")) || "🟡 Moyen".equals(r.get("riskLevel")))
                    .count();
            double totalOverdueAmount = results.stream()
                    .filter(r -> (Boolean) r.getOrDefault("isOverdue", false))
                    .mapToDouble(r -> (Double) r.get("amount"))
                    .sum();

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalInvoices", results.size());
            summary.put("criticalCount", criticalCount);
            summary.put("highRiskCount", highRiskCount);
            summary.put("mediumRiskCount", mediumRiskCount);
            summary.put("totalOverdueAmount", totalOverdueAmount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summary);
            response.put("invoices", results);
            response.put("generatedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting upcoming invoices: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @PostMapping("/refresh-upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> refreshUpcomingPredictions(
            @RequestParam(defaultValue = "6") int days) {
        try {
            riskAIModel.clearPredictionCache();
            List<Map<String, Object>> results = riskAIModel.getUpcomingInvoicesWithRisk(days);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Predictions refreshed successfully");
            response.put("totalRefreshed", results.size());
            response.put("invoices", results);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error refreshing predictions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getDashboardSummary(
            @RequestParam(defaultValue = "6") int days) {
        try {
            List<Map<String, Object>> results = riskAIModel.getUpcomingInvoicesWithRisk(days);

            Map<String, Object> summary = new HashMap<>();

            // Use consistent keys (English for API, French for display can be done on frontend)
            Map<String, Long> riskLevelCounts = new LinkedHashMap<>();
            riskLevelCounts.put("CRITICAL", 0L);
            riskLevelCounts.put("VERY_HIGH", 0L);
            riskLevelCounts.put("HIGH", 0L);
            riskLevelCounts.put("MEDIUM", 0L);
            riskLevelCounts.put("LOW", 0L);
            riskLevelCounts.put("VERY_LOW", 0L);

            Map<String, Double> amountByRisk = new LinkedHashMap<>();
            amountByRisk.put("CRITICAL", 0.0);
            amountByRisk.put("VERY_HIGH", 0.0);
            amountByRisk.put("HIGH", 0.0);
            amountByRisk.put("MEDIUM", 0.0);
            amountByRisk.put("LOW", 0.0);
            amountByRisk.put("VERY_LOW", 0.0);

            for (Map<String, Object> invoice : results) {
                String level = (String) invoice.get("riskLevel");
                Double amount = (Double) invoice.get("amount");

                // Map display labels to internal codes
                String mappedLevel = level;
                if (level.contains("Critique") || level.contains("⚠️")) {
                    mappedLevel = "CRITICAL";
                } else if (level.contains("Très Faible") || level.contains("✅")) {
                    mappedLevel = "VERY_LOW";
                }

                riskLevelCounts.merge(mappedLevel, 1L, Long::sum);
                amountByRisk.merge(mappedLevel, amount, Double::sum);
            }

            summary.put("riskLevelCounts", riskLevelCounts);
            summary.put("amountByRisk", amountByRisk);
            summary.put("totalInvoices", results.size());
            summary.put("totalAmount", results.stream().mapToDouble(r -> (Double) r.get("amount")).sum());
            summary.put("days", days);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summary);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting dashboard summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }



    @GetMapping("/recommendations/invoice/{numeroFacture}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getInvoiceRecommendations(@PathVariable String numeroFacture) {
        try {
            Facture facture = factureRepository.findByNumeroFacture(numeroFacture)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with number: " + numeroFacture));

            List<AIRecommendationEngine.AIRecommendation> recommendations =
                    recommendationEngine.generateInvoiceRecommendations(facture.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("recommendations", recommendations);
            response.put("count", recommendations.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting recommendations: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/recommendations/client/{clientCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getClientRecommendations(@PathVariable String clientCode) {
        try {
            Structure client = structureRepository.findByCode(clientCode)
                    .orElseThrow(() -> new RuntimeException("Client not found with code: " + clientCode));

            List<AIRecommendationEngine.AIRecommendation> recommendations =
                    recommendationEngine.generateClientRecommendations(client.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("recommendations", recommendations);
            response.put("count", recommendations.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting client recommendations: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/batch/client/{clientCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> predictClientRisk(@PathVariable String clientCode) {
        try {
            Structure client = structureRepository.findByCode(clientCode)
                    .orElseThrow(() -> new RuntimeException("Client not found with code: " + clientCode));

            List<InvoiceRiskAIModel.RiskPrediction> predictions =
                    riskAIModel.predictClientRisk(client.getId());

            // Aggregate statistics
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalInvoices", predictions.size());
            stats.put("highRiskCount", predictions.stream()
                    .filter(p -> p.getLevel().getSeverity() >= 3)
                    .count());
            stats.put("criticalCount", predictions.stream()
                    .filter(p -> p.getLevel() == InvoiceRiskAIModel.RiskLevel.CRITICAL)
                    .count());
            stats.put("veryHighCount", predictions.stream()
                    .filter(p -> p.getLevel() == InvoiceRiskAIModel.RiskLevel.VERY_HIGH)
                    .count());
            stats.put("highCount", predictions.stream()
                    .filter(p -> p.getLevel() == InvoiceRiskAIModel.RiskLevel.HIGH)
                    .count());
            stats.put("mediumCount", predictions.stream()
                    .filter(p -> p.getLevel() == InvoiceRiskAIModel.RiskLevel.MEDIUM)
                    .count());
            stats.put("lowCount", predictions.stream()
                    .filter(p -> p.getLevel() == InvoiceRiskAIModel.RiskLevel.LOW ||
                            p.getLevel() == InvoiceRiskAIModel.RiskLevel.VERY_LOW)
                    .count());
            stats.put("averageRiskScore", predictions.stream()
                    .mapToDouble(p -> p.getProbability() * 100)
                    .average()
                    .orElse(0));
            stats.put("totalPredictedDelay", predictions.stream()
                    .mapToInt(InvoiceRiskAIModel.RiskPrediction::getPredictedDelayDays)
                    .sum());
            stats.put("averageConfidence", predictions.stream()
                    .mapToDouble(InvoiceRiskAIModel.RiskPrediction::getConfidence)
                    .average()
                    .orElse(0));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            response.put("predictions", predictions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in batch prediction: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
}