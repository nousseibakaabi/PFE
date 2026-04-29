package com.example.back.service.ai;

import com.example.back.entity.*;
import com.example.back.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class AIRecommendationEngine {

    private final FactureRepository factureRepository;
    private final ConventionRepository conventionRepository;
    private final InvoiceRiskAIModel riskAIModel;

    public AIRecommendationEngine(FactureRepository factureRepository,
                                  ConventionRepository conventionRepository,
                                  InvoiceRiskAIModel riskAIModel) {
        this.factureRepository = factureRepository;
        this.conventionRepository = conventionRepository;
        this.riskAIModel = riskAIModel;
    }

    /**
     * Generate intelligent recommendations based on AI prediction
     */
    public List<AIRecommendation> generateInvoiceRecommendations(Long factureId) {
        List<AIRecommendation> recommendations = new ArrayList<>();

        Facture facture = factureRepository.findById(factureId).orElse(null);
        if (facture == null || "PAYE".equals(facture.getStatutPaiement())) {
            return recommendations;
        }

        // Get AI prediction first
        InvoiceRiskAIModel.RiskPrediction prediction = riskAIModel.predictInvoiceRisk(factureId);

        // Get features for additional context
        InvoiceRiskAIModel.HistoricalPaymentData features = riskAIModel.extractFeaturesFromFacture(facture);

        // ============ AI-BASED RECOMMENDATIONS ============

        // 1. Recommendation based on AI Risk Level
        switch (prediction.getLevel()) {
            case CRITICAL:
                recommendations.add(createAIRecRecommendation(
                        "CRITICAL_RISK_" + factureId,
                        "🚨 ACTION IMMÉDIATE REQUISE - Risque Critique",
                        "L'IA a détecté un risque de non-paiement TRÈS ÉLEVÉ avec une confiance de " +
                                String.format("%.0f", prediction.getConfidence() * 100) + "%",
                        "URGENT",
                        LocalDate.now(),
                        "📞 Contacter le service financier du client dans les 24h\n" +
                                "🔒 Envisager la suspension temporaire des services\n" +
                                "⚖️ Préparer le dossier pour recouvrement\n" +
                                "📧 Envoyer une mise en demeure immédiate"
                ));
                break;

            case VERY_HIGH:
                recommendations.add(createAIRecRecommendation(
                        "VERY_HIGH_RISK_" + factureId,
                        "🔴 Risque Très Élevé - Action Requise",
                        "L'IA prédit un risque important avec une probabilité de " +
                                String.format("%.0f", prediction.getProbability() * 100) + "%",
                        "HIGH",
                        LocalDate.now(),
                        "📞 Planifier un appel avec le responsable financier sous 48h\n" +
                                "📧 Envoyer une relance personnalisée avec rappel des conditions\n" +
                                "🔍 Demander un acompte pour les futures factures"
                ));
                break;

            case HIGH:
                recommendations.add(createAIRecRecommendation(
                        "HIGH_RISK_" + factureId,
                        "⚠️ Risque Élevé - Surveillance Renforcée",
                        "Facture avec score de risque de " + String.format("%.0f", features.riskScore) + "%",
                        "HIGH",
                        LocalDate.now(),
                        "📧 Envoyer une relance à J-7 et J-3\n" +
                                "📞 Appel de courtoisie pour confirmer la réception\n" +
                                "📊 Ajouter à la liste de surveillance mensuelle"
                ));
                break;

            case MEDIUM:
                recommendations.add(createAIRecRecommendation(
                        "MEDIUM_RISK_" + factureId,
                        "📋 Risque Moyen - Suivi Standard",
                        "Risque modéré détecté - Surveillance recommandée",
                        "MEDIUM",
                        LocalDate.now(),
                        "⏰ Programmer un rappel automatique à J-7\n" +
                                "📧 Envoyer une relance standard à J-3\n" +
                                "✅ Suivi normal sans action immédiate"
                ));
                break;

            case LOW:
                recommendations.add(createAIRecRecommendation(
                        "LOW_RISK_" + factureId,
                        "✅ Risque Faible - Client Fiable",
                        "Client avec bon historique de paiement",
                        "LOW",
                        LocalDate.now(),
                        "📧 Envoyer une relance standard uniquement\n" +
                                "✅ Aucune action spéciale requise"
                ));
                break;

            case VERY_LOW:
                recommendations.add(createAIRecRecommendation(
                        "VERY_LOW_RISK_" + factureId,
                        "✨ Très Faible Risque - Excellent Client",
                        "Client très fiable - Aucun risque détecté",
                        "LOW",
                        LocalDate.now(),
                        "📧 Envoyer uniquement un rappel de paiement\n" +
                                "🎯 Proposer des conditions préférentielles pour fidéliser"
                ));
                break;
        }

        // 2. Recommendation based on overdue days from AI prediction
        if (prediction.getPredictedDelayDays() > 0) {
            recommendations.add(createAIRecRecommendation(
                    "PREDICTED_DELAY_" + factureId,
                    "📅 Risque de retard estimé",
                    "L'IA prédit un risque de retard de " + prediction.getPredictedDelayDays() + " jours",
                    prediction.getPredictedDelayDays() > 30 ? "HIGH" : "MEDIUM",
                    LocalDate.now(),
                    generateDelayAction(prediction.getPredictedDelayDays())
            ));
        }

        // 3. Recommendation based on client payment history (from AI features)
        if (features.latePaymentRate > 0.3) {
            recommendations.add(createAIRecRecommendation(
                    "HISTORY_LATE_" + factureId,
                    "📊 Historique de retards détecté",
                    "Ce client a un historique de " + String.format("%.0f", features.latePaymentRate * 100) + "% de paiements en retard",
                    "HIGH",
                    LocalDate.now(),
                    "📞 Contacter le client pour comprendre les causes des retards\n" +
                            "📝 Proposer un échéancier de paiement adapté\n" +
                            "🔒 Réduire les délais de paiement pour les futures factures"
            ));
        }

        if (features.paymentOnTimeRate > 0.9) {
            recommendations.add(createAIRecRecommendation(
                    "HISTORY_GOOD_" + factureId,
                    "🏆 Excellent historique de paiement",
                    "Ce client paie ses factures à temps dans " + String.format("%.0f", features.paymentOnTimeRate * 100) + "% des cas",
                    "LOW",
                    LocalDate.now(),
                    "🎁 Offrir des conditions commerciales préférentielles\n" +
                            "⭐ Marquer comme client prioritaire\n" +
                            "📧 Envoyer uniquement des rappels standards"
            ));
        }

        // 4. Recommendation based on new client status
        if (features.totalConventions == 1 && features.paymentOnTimeRate == 0 && features.latePaymentRate == 0) {
            recommendations.add(createAIRecRecommendation(
                    "NEW_CLIENT_" + factureId,
                    "🆕 Nouveau Client - Suivi Renforcé",
                    "Première facture pour ce client - Aucun historique disponible",
                    "MEDIUM",
                    LocalDate.now(),
                    "📞 Appeler le client pour confirmer la réception de la facture\n" +
                            "📧 Envoyer un email de bienvenue avec rappel des conditions\n" +
                            "✅ Assurer un suivi personnalisé pour les premières factures"
            ));
        }

        // 5. Recommendation based on invoice amount (using AI prediction context)
        BigDecimal amount = facture.getMontantTTC();
        if (amount != null && amount.compareTo(new BigDecimal("50000")) > 0 && prediction.getLevel().getSeverity() >= 2) {
            recommendations.add(createAIRecRecommendation(
                    "HIGH_AMOUNT_RISK_" + factureId,
                    "💰 Montant Élevé + Risque Détecté",
                    "Facture de " + String.format("%,.2f", amount) + " TND avec risque " + prediction.getLevel().getLabel(),
                    "URGENT",
                    LocalDate.now(),
                    "📞 Contacter la direction financière directement\n" +
                            "📝 Demander un acompte de 50% minimum\n" +
                            "🔒 Renforcer les garanties de paiement"
            ));
        }

        // 6. Add the original AI prediction recommendations
        if (prediction.getRecommendations() != null && !prediction.getRecommendations().isEmpty()) {
            for (String rec : prediction.getRecommendations()) {
                // Don't duplicate if already added
                if (!rec.contains("ACTION IMMÉDIATE") && !rec.contains("Client fiable")) {
                    recommendations.add(createAIRecRecommendation(
                            "AI_REC_" + UUID.randomUUID().toString(),
                            "💡 Recommandation IA",
                            rec,
                            getPriorityFromRecommendation(rec),
                            LocalDate.now(),
                            rec
                    ));
                }
            }
        }

        // 7. Due date based reminders (only if no critical risk already)
        if (prediction.getLevel().getSeverity() < 4 && facture.getDateEcheance() != null) {
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), facture.getDateEcheance());

            if (daysUntilDue == 7) {
                recommendations.add(createAIRecRecommendation(
                        "DUE_7_DAYS_" + factureId,
                        "📅 Échéance dans 7 jours",
                        "Rappel automatique avant échéance",
                        "LOW",
                        LocalDate.now().plusDays(1),
                        "📧 Envoyer un email de rappel standard"
                ));
            } else if (daysUntilDue == 3 && prediction.getLevel().getSeverity() >= 2) {
                recommendations.add(createAIRecRecommendation(
                        "DUE_3_DAYS_" + factureId,
                        "⏰ Échéance dans 3 jours",
                        "Rappel à J-3 pour client à risque",
                        "MEDIUM",
                        LocalDate.now(),
                        "📧 Envoyer un rappel personnalisé\n📞 Appel de confirmation"
                ));
            } else if (daysUntilDue < 0 && prediction.getLevel().getSeverity() < 4) {
                long daysOverdue = -daysUntilDue;
                recommendations.add(createAIRecRecommendation(
                        "OVERDUE_" + daysOverdue + "_" + factureId,
                        "🔴 Facture en retard de " + daysOverdue + " jours",
                        generateOverdueMessage(daysOverdue),
                        getOverduePriority(daysOverdue),
                        LocalDate.now(),
                        generateOverdueAction(daysOverdue)
                ));
            }
        }

        return recommendations;
    }

    /**
     * Generate strategic recommendations for a client based on AI predictions
     */
    public List<AIRecommendation> generateClientRecommendations(Long clientId) {
        List<AIRecommendation> recommendations = new ArrayList<>();

        List<InvoiceRiskAIModel.RiskPrediction> predictions = riskAIModel.predictClientRisk(clientId);

        if (predictions.isEmpty()) {
            return recommendations;
        }

        // Analyze payment behavior from AI predictions
        double avgRiskScore = predictions.stream()
                .mapToDouble(p -> p.getProbability() * 100)
                .average()
                .orElse(0);

        long criticalCount = predictions.stream()
                .filter(p -> p.getLevel() == InvoiceRiskAIModel.RiskLevel.CRITICAL)
                .count();

        long veryHighCount = predictions.stream()
                .filter(p -> p.getLevel() == InvoiceRiskAIModel.RiskLevel.VERY_HIGH)
                .count();

        long highCount = predictions.stream()
                .filter(p -> p.getLevel() == InvoiceRiskAIModel.RiskLevel.HIGH)
                .count();

        long totalHighRisk = criticalCount + veryHighCount + highCount;

        long totalOverdue = predictions.stream()
                .filter(p -> p.getPredictedDelayDays() > 0)
                .count();

        // Strategic recommendations based on AI analysis
        if (criticalCount > 0) {
            recommendations.add(createAIRecRecommendation(
                    "CLIENT_CRITICAL_" + clientId,
                    "🚨 CLIENT À RISQUE CRITIQUE",
                    criticalCount + " facture(s) en situation critique selon l'IA",
                    "URGENT",
                    LocalDate.now(),
                    "📞 Contacter immédiatement la direction\n" +
                            "⚖️ Préparer le dossier pour action juridique\n" +
                            "🔒 Suspendre les services si nécessaire\n" +
                            "📊 Réunion d'urgence avec l'équipe commerciale"
            ));
        }

        if (avgRiskScore > 60) {
            recommendations.add(createAIRecRecommendation(
                    "CLIENT_STRATEGIC_RISK_" + clientId,
                    "📊 Plan de recouvrement renforcé",
                    "Score de risque moyen de " + String.format("%.1f", avgRiskScore) + "% - Client prioritaire",
                    "HIGH",
                    LocalDate.now(),
                    "📝 Mettre en place un plan de recouvrement personnalisé\n" +
                            "📞 Assigner un commercial dédié\n" +
                            "🔒 Réduire les délais de paiement à 30 jours\n" +
                            "💰 Demander des acomptes pour les nouvelles commandes"
            ));
        }

        if (totalHighRisk > 3) {
            recommendations.add(createAIRecRecommendation(
                    "MULTIPLE_RISK_INVOICES_" + clientId,
                    "📑 Multiples factures à risque",
                    totalHighRisk + " factures présentent un risque élevé ou critique",
                    "URGENT",
                    LocalDate.now(),
                    "🏢 Organiser une réunion avec la direction financière du client\n" +
                            "📊 Présenter un état des lieux des impayés\n" +
                            "📝 Proposer un échéancier global\n" +
                            "⚖️ Préparer une mise en demeure globale"
            ));
        }

        if (totalOverdue > 5) {
            recommendations.add(createAIRecRecommendation(
                    "RECURRING_OVERDUE_" + clientId,
                    "⏰ Retards récurrents détectés",
                    totalOverdue + " factures avec des retards de paiement prévus",
                    "HIGH",
                    LocalDate.now(),
                    "📞 Organiser un point d'étape mensuel\n" +
                            "💰 Négocier un dépôt de garantie\n" +
                            "📝 Réviser les conditions de paiement\n" +
                            "🔍 Audit du processus de paiement client"
            ));
        }

        if (totalHighRisk == 0 && avgRiskScore < 30) {
            recommendations.add(createAIRecRecommendation(
                    "GOOD_CLIENT_" + clientId,
                    "✅ Client Fiable - À Fidéliser",
                    "Excellent historique de paiement - Aucune facture à risque",
                    "LOW",
                    LocalDate.now(),
                    "🎁 Offrir des conditions commerciales préférentielles\n" +
                            "⭐ Statut VIP - Priorité au traitement\n" +
                            "📧 Envoi de factures avec délais prolongés\n" +
                            "🎯 Programme de fidélisation personnalisé"
            ));
        }

        return recommendations;
    }

    private AIRecommendation createAIRecRecommendation(String id, String title, String description,
                                                       String priority, LocalDate dueDate, String action) {
        AIRecommendation rec = new AIRecommendation();
        rec.setId(id);
        rec.setTitle(title);
        rec.setDescription(description);
        rec.setPriority(priority);
        rec.setDueDate(dueDate);
        rec.setAction(action);
        rec.setGeneratedAt(LocalDate.now());
        return rec;
    }

    private String generateDelayAction(int predictedDelayDays) {
        if (predictedDelayDays <= 7) {
            return "📧 Envoyer un rappel à J-3 et J-1\n" +
                    "📞 Appel de confirmation la veille de l'échéance";
        } else if (predictedDelayDays <= 30) {
            return "📧 Envoyer une relance à J-7\n" +
                    "📞 Appel à J-3 pour confirmer le paiement\n" +
                    "📊 Ajouter au suivi hebdomadaire";
        } else {
            return "📞 Contacter immédiatement le client\n" +
                    "📧 Envoyer une mise en demeure\n" +
                    "⚖️ Consulter le service juridique\n" +
                    "🔒 Envisager la suspension des services";
        }
    }

    private String generateOverdueMessage(long daysOverdue) {
        if (daysOverdue <= 7) {
            return "Premier retard - Action rapide requise";
        } else if (daysOverdue <= 30) {
            return "Retard significatif - Relance renforcée nécessaire";
        } else {
            return "Retard critique - Action juridique à envisager";
        }
    }

    private String generateOverdueAction(long daysOverdue) {
        if (daysOverdue <= 7) {
            return "📧 Envoyer une première relance\n" +
                    "📞 Appel de courtoisie";
        } else if (daysOverdue <= 30) {
            return "📧 Envoyer une deuxième relance\n" +
                    "📞 Appel formel au service comptable\n" +
                    "📝 Envoyer une lettre de rappel";
        } else {
            return "⚖️ Préparer une mise en demeure\n" +
                    "📞 Contacter la direction\n" +
                    "🔒 Suspendre les services\n" +
                    "📝 Transmettre au contentieux";
        }
    }

    private String getOverduePriority(long daysOverdue) {
        if (daysOverdue <= 7) return "MEDIUM";
        if (daysOverdue <= 30) return "HIGH";
        return "URGENT";
    }

    private String getPriorityFromRecommendation(String recommendation) {
        if (recommendation.contains("ACTION IMMÉDIATE") || recommendation.contains("CRITICAL")) {
            return "URGENT";
        } else if (recommendation.contains("ÉLEVÉ") || recommendation.contains("HIGH")) {
            return "HIGH";
        } else if (recommendation.contains("MOYEN")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    // Recommendation DTO
    public static class AIRecommendation {
        private String id;
        private String title;
        private String description;
        private String priority;
        private LocalDate dueDate;
        private String action;
        private LocalDate generatedAt;
        private boolean executed;
        private LocalDate executedAt;
        private String executedBy;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public LocalDate getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDate generatedAt) { this.generatedAt = generatedAt; }
        public boolean isExecuted() { return executed; }
        public void setExecuted(boolean executed) { this.executed = executed; }
        public LocalDate getExecutedAt() { return executedAt; }
        public void setExecutedAt(LocalDate executedAt) { this.executedAt = executedAt; }
        public String getExecutedBy() { return executedBy; }
        public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    }
}