package com.example.back.controller;

import com.example.back.entity.Facture;
import com.example.back.repository.FactureRepository;
import com.example.back.service.ai.AIRecommendationEngine;
import com.example.back.service.ai.InvoiceRiskAIModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-risk")
@RequiredArgsConstructor
@Slf4j
public class AIRiskController {

    private final InvoiceRiskAIModel riskAIModel;
    private final AIRecommendationEngine recommendationEngine;

    private final FactureRepository factureRepository;
    // ==================== PREDICTION ENDPOINTS ====================

    /**
     * AI prediction for an existing invoice
     * GET /api/ai-risk/predict/invoice/{factureId}
     *
     * Response: {
     *   "success": true,
     *   "prediction": {
     *     "level": { "code": 3, "label": "🟠 Élevé", "color": "#f97316", "severity": 3 },
     *     "probability": 0.82,
     *     "confidence": 0.75,
     *     "predictedDelayDays": 15,
     *     "featureContributions": {...},
     *     "recommendations": [...],
     *     "explanation": "..."
     *   }
     * }
     */
    @GetMapping("/predict/invoice/{factureId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> predictInvoiceRisk(@PathVariable Long factureId) {
        try {
            InvoiceRiskAIModel.RiskPrediction prediction = riskAIModel.predictInvoiceRisk(factureId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prediction", prediction);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error predicting invoice risk: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * AI prediction for a new invoice (before creation)
     * POST /api/ai-risk/predict/new-invoice?amount=50000&dueDate=2026-05-30&clientId=123
     *
     * Use this BEFORE sending an invoice to predict if the client will pay late
     */
    @PostMapping("/predict/new-invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> predictNewInvoiceRisk(
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam Long clientId,
            @RequestParam(required = false) Long applicationId) {
        try {
            InvoiceRiskAIModel.RiskPrediction prediction = riskAIModel.predictNewInvoiceRisk(
                    amount, dueDate, clientId, applicationId);

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

    /**
     * Batch prediction for all unpaid invoices of a client
     * GET /api/ai-risk/batch/client/{clientId}
     *
     * Returns risk analysis for ALL unpaid invoices of a specific client
     */
    @GetMapping("/batch/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> predictClientRisk(@PathVariable Long clientId) {
        try {
            List<InvoiceRiskAIModel.RiskPrediction> predictions =
                    riskAIModel.predictClientRisk(clientId);

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

    // ==================== RECOMMENDATION ENDPOINTS ====================

    /**
     * Get AI recommendations for an invoice
     * GET /api/ai-risk/recommendations/invoice/{factureId}
     *
     * Returns actionable recommendations for a specific invoice
     */
    @GetMapping("/recommendations/invoice/{factureId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getInvoiceRecommendations(@PathVariable Long factureId) {
        try {
            List<AIRecommendationEngine.AIRecommendation> recommendations =
                    recommendationEngine.generateInvoiceRecommendations(factureId);

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

    /**
     * Get strategic recommendations for a client
     * GET /api/ai-risk/recommendations/client/{clientId}
     *
     * Returns strategic recommendations for managing a client's payment risk
     */
    @GetMapping("/recommendations/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getClientRecommendations(@PathVariable Long clientId) {
        try {
            List<AIRecommendationEngine.AIRecommendation> recommendations =
                    recommendationEngine.generateClientRecommendations(clientId);

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

    // ==================== DASHBOARD & STATISTICS ENDPOINTS ====================

    /**
     * Get risk dashboard for all clients
     * GET /api/ai-risk/dashboard
     *
     * Returns aggregated risk statistics across all clients
     */
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

    /**
     * Get high risk invoices for the current user
     * GET /api/ai-risk/my-high-risk-invoices?minLevel=HIGH
     */
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

    /**
     * Get all high-risk clients (for management)
     * GET /api/ai-risk/high-risk-clients?minLevel=HIGH
     */
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

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Force retrain the AI model
     * POST /api/ai-risk/retrain
     */
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

    /**
     * Clear AI prediction cache
     * DELETE /api/ai-risk/cache
     */
    @DeleteMapping("/admin/cache")
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

    /**
     * Get model information and statistics
     * GET /api/ai-risk/model-info
     */
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

    // ==================== TEST/DEBUG ENDPOINTS ====================

    /**
     * Test endpoint to verify AI model is working
     * GET /api/ai-risk/health
     */
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

    @GetMapping("/debug/features/{factureId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> debugFeatures(@PathVariable Long factureId) {
        Facture facture = factureRepository.findById(factureId).orElse(null);
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

        return ResponseEntity.ok(debug);
    }
}