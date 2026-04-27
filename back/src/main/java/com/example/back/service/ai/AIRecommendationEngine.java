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
     * Generate intelligent recommendations for a specific invoice
     */
    public List<AIRecommendation> generateInvoiceRecommendations(Long factureId) {
        List<AIRecommendation> recommendations = new ArrayList<>();

        Facture facture = factureRepository.findById(factureId).orElse(null);
        if (facture == null || "PAYE".equals(facture.getStatutPaiement())) {
            return recommendations;
        }

        InvoiceRiskAIModel.RiskPrediction prediction = riskAIModel.predictInvoiceRisk(factureId);

        // Recommendation 1: Timing-based reminders
        if (facture.getDateEcheance() != null) {
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), facture.getDateEcheance());

            if (daysUntilDue == 7) {
                recommendations.add(createRecommendation(
                        "REMINDER_7_DAYS",
                        "📅 Relance J-7",
                        "Envoyer un rappel de paiement 7 jours avant l'échéance",
                        "MEDIUM",
                        LocalDate.now().plusDays(1),
                        generateReminderTemplate(facture, daysUntilDue)
                ));
            } else if (daysUntilDue == 3) {
                recommendations.add(createRecommendation(
                        "REMINDER_3_DAYS",
                        "⏰ Relance J-3",
                        "Rappel à 3 jours de l'échéance - Contacter le client",
                        "HIGH",
                        LocalDate.now(),
                        generateReminderTemplate(facture, daysUntilDue)
                ));
            } else if (daysUntilDue == 0) {
                recommendations.add(createRecommendation(
                        "DUE_TODAY",
                        "⚠️ Échéance aujourd'hui",
                        "La facture arrive à échéance aujourd'hui - Action requise",
                        "URGENT",
                        LocalDate.now(),
                        generateDueTodayTemplate(facture)
                ));
            } else if (daysUntilDue < 0) {
                long daysOverdue = -daysUntilDue;
                recommendations.add(createRecommendation(
                        "OVERDUE_" + daysOverdue,
                        "🔴 Retard de " + daysOverdue + " jours",
                        generateOverdueMessage(daysOverdue),
                        getUrgencyLevel(daysOverdue),
                        LocalDate.now(),
                        generateOverdueTemplate(facture, daysOverdue)
                ));
            }
        }

        // Recommendation 2: Client-specific actions based on AI prediction
        if (prediction.getLevel().getSeverity() >= 3) { // HIGH or above
            recommendations.add(createRecommendation(
                    "CLIENT_HIGH_RISK",
                    "⚠️ Client à risque élevé",
                    "Ce client a un score de risque de " + String.format("%.0f", prediction.getProbability() * 100) + "%",
                    "HIGH",
                    LocalDate.now(),
                    "Contacter immédiatement le client par téléphone pour confirmer la réception et le paiement"
            ));
        }

        // Recommendation 3: Financial action
        BigDecimal amount = facture.getMontantTTC();
        if (amount != null && amount.compareTo(new java.math.BigDecimal("50000")) > 0) {
            recommendations.add(createRecommendation(
                    "HIGH_AMOUNT",
                    "💰 Montant élevé",
                    "Facture de " + String.format("%,.2f", amount) + " TND",
                    "MEDIUM",
                    LocalDate.now(),
                    "Demander confirmation de réception et relance personnalisée"
            ));
        }

        return recommendations;
    }

    /**
     * Generate strategic recommendations for a client
     */
    public List<AIRecommendation> generateClientRecommendations(Long clientId) {
        List<AIRecommendation> recommendations = new ArrayList<>();

        List<InvoiceRiskAIModel.RiskPrediction> predictions = riskAIModel.predictClientRisk(clientId);

        // Analyze payment behavior
        double avgRiskScore = predictions.stream()
                .mapToDouble(p -> p.getProbability() * 100)
                .average()
                .orElse(0);

        long highRiskInvoices = predictions.stream()
                .filter(p -> p.getLevel().getSeverity() >= 3)
                .count();

        long totalOverdue = predictions.stream()
                .filter(p -> p.getPredictedDelayDays() > 0)
                .count();

        if (avgRiskScore > 60) {
            recommendations.add(createRecommendation(
                    "CLIENT_STRATEGIC_RISK",
                    "📊 Stratégie de recouvrement",
                    "Client avec score de risque moyen de " + String.format("%.1f", avgRiskScore) + "%",
                    "HIGH",
                    LocalDate.now(),
                    "Mettre en place un plan de recouvrement renforcé pour ce client"
            ));
        }

        if (highRiskInvoices > 3) {
            recommendations.add(createRecommendation(
                    "MULTIPLE_RISK_INVOICES",
                    "📑 Multiples factures à risque",
                    highRiskInvoices + " factures présentent un risque élevé",
                    "URGENT",
                    LocalDate.now(),
                    "Organiser une réunion avec la direction financière du client"
            ));
        }

        if (totalOverdue == 0 && highRiskInvoices == 0) {
            recommendations.add(createRecommendation(
                    "GOOD_CLIENT",
                    "✅ Client fiable",
                    "Excellent historique de paiement - Aucune facture à risque",
                    "LOW",
                    LocalDate.now(),
                    "Proposer des conditions commerciales préférentielles pour fidéliser"
            ));
        }

        return recommendations;
    }

    private AIRecommendation createRecommendation(String id, String title, String description,
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

    private String generateReminderTemplate(Facture facture, long daysUntilDue) {
        return String.format(
                "Objet: Rappel - Facture %s\n\n" +
                        "Bonjour,\n\n" +
                        "Nous vous rappelons que la facture %s d'un montant de %.2f TND " +
                        "arrive à échéance dans %d jours (le %s).\n\n" +
                        "Nous restons à votre disposition pour tout complément d'information.\n\n" +
                        "Cordialement,\nService Client",
                facture.getNumeroFacture(),
                facture.getNumeroFacture(),
                facture.getMontantTTC() != null ? facture.getMontantTTC() : 0,
                daysUntilDue,
                facture.getDateEcheance()
        );
    }

    private String generateDueTodayTemplate(Facture facture) {
        return String.format(
                "Objet: URGENT - Échéance aujourd'hui - Facture %s\n\n" +
                        "Bonjour,\n\n" +
                        "La facture %s d'un montant de %.2f TND arrive à échéance AUJOURD'HUI (%s).\n\n" +
                        "Merci de bien vouloir procéder au règlement dans les meilleurs délais.\n\n" +
                        "Cordialement,\nService Comptabilité",
                facture.getNumeroFacture(),
                facture.getNumeroFacture(),
                facture.getMontantTTC() != null ? facture.getMontantTTC() : 0,
                facture.getDateEcheance()
        );
    }

    private String generateOverdueTemplate(Facture facture, long daysOverdue) {
        String template;
        if (daysOverdue <= 7) {
            template = String.format(
                    "Objet: RELANCE - Facture %s en retard\n\n" +
                            "Bonjour,\n\n" +
                            "Nous constatons que la facture %s d'un montant de %.2f TND " +
                            "est en retard de %d jours.\n\n" +
                            "Nous vous remercions de bien vouloir régulariser cette situation " +
                            "dans les plus brefs délais.\n\n" +
                            "Cordialement,\nService Comptabilité",
                    facture.getNumeroFacture(),
                    facture.getNumeroFacture(),
                    facture.getMontantTTC() != null ? facture.getMontantTTC() : 0,
                    daysOverdue
            );
        } else if (daysOverdue <= 30) {
            template = String.format(
                    "Objet: 2ème RELANCE - Facture %s en retard de %d jours\n\n" +
                            "Bonjour,\n\n" +
                            "Malgré notre précédent rappel, la facture %s d'un montant de %.2f TND " +
                            "reste impayée depuis %d jours.\n\n" +
                            "Nous vous demandons de bien vouloir procéder au règlement immédiat " +
                            "pour éviter toute interruption de service.\n\n" +
                            "Cordialement,\nService Comptabilité",
                    facture.getNumeroFacture(), daysOverdue,
                    facture.getNumeroFacture(),
                    facture.getMontantTTC() != null ? facture.getMontantTTC() : 0,
                    daysOverdue
            );
        } else {
            template = String.format(
                    "Objet: MISE EN DEMEURE - Facture %s - Retard de %d jours\n\n" +
                            "Bonjour,\n\n" +
                            "Malgré nos relances précédentes, la facture %s d'un montant de %.2f TND " +
                            "reste impayée depuis %d jours.\n\n" +
                            "Nous vous mettons en demeure de régler cette facture sous 48h, " +
                            "faute de quoi nous serons contraints d'engager des procédures de recouvrement.\n\n" +
                            "Cordialement,\nService Contentieux",
                    facture.getNumeroFacture(), daysOverdue,
                    facture.getNumeroFacture(),
                    facture.getMontantTTC() != null ? facture.getMontantTTC() : 0,
                    daysOverdue
            );
        }
        return template;
    }

    private String generateOverdueMessage(long daysOverdue) {
        if (daysOverdue <= 7) return "Premier retard - Envoyer relance standard";
        if (daysOverdue <= 30) return "Retard significatif - Envoyer 2ème relance";
        return "Retard critique - Action juridique à envisager";
    }

    private String getUrgencyLevel(long daysOverdue) {
        if (daysOverdue <= 7) return "MEDIUM";
        if (daysOverdue <= 30) return "HIGH";
        return "URGENT";
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