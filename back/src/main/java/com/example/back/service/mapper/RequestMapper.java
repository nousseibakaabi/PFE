package com.example.back.service.mapper;

import com.example.back.entity.Request;
import com.example.back.payload.response.RequestResponse;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class RequestMapper {

    public RequestResponse toResponse(Request request) {
        RequestResponse response = new RequestResponse();

        response.setId(request.getId());
        response.setRequestType(request.getRequestType());
        response.setStatus(request.getStatus());

        // Requester info
        if (request.getRequester() != null) {
            response.setRequesterId(request.getRequester().getId());
            response.setRequesterName(request.getRequester().getFirstName() + " " + request.getRequester().getLastName());
            response.setRequesterEmail(request.getRequester().getEmail());
        }

        // Target user info
        if (request.getTargetUser() != null) {
            response.setTargetUserId(request.getTargetUser().getId());
            response.setTargetUserName(request.getTargetUser().getFirstName() + " " + request.getTargetUser().getLastName());
            response.setTargetUserEmail(request.getTargetUser().getEmail());
        }

        // Application info
        if (request.getApplication() != null) {
            response.setApplicationId(request.getApplication().getId());
            response.setApplicationName(request.getApplication().getName());
            response.setApplicationCode(request.getApplication().getCode());
        }

        // Convention info
        if (request.getConvention() != null) {
            response.setConventionId(request.getConvention().getId());
            response.setConventionReference(request.getConvention().getReferenceConvention());
            response.setConventionLibelle(request.getConvention().getLibelle());
        }

        // Old convention info
        if (request.getOldConvention() != null) {
            response.setOldConventionId(request.getOldConvention().getId());
            response.setOldConventionReference(request.getOldConvention().getReferenceConvention());
        }

        // Recommended chef info
        if (request.getRecommendedChef() != null) {
            response.setRecommendedChefId(request.getRecommendedChef().getId());
            response.setRecommendedChefName(request.getRecommendedChef().getFirstName() + " " + request.getRecommendedChef().getLastName());
            response.setRecommendedChefEmail(request.getRecommendedChef().getEmail());
        }

        response.setReason(request.getReason());
        response.setDenialReason(request.getDenialReason());
        response.setRecommendations(request.getRecommendations());
        response.setCreatedAt(request.getCreatedAt());
        response.setProcessedAt(request.getProcessedAt());

        // Calculate time ago
        response.setTimeAgo(calculateTimeAgo(request.getCreatedAt()));

        // UI helpers
        response.setStatusColor(getStatusColor(request.getStatus()));
        response.setStatusIcon(getStatusIcon(request.getStatus()));
        response.setTypeLabel(getTypeLabel(request.getRequestType()));

        return response;
    }

    private String calculateTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);

        if (minutes < 1) return "À l'instant";
        if (minutes < 60) return "Il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        if (hours < 24) return "Il y a " + hours + " heure" + (hours > 1 ? "s" : "");
        if (days < 7) return "Il y a " + days + " jour" + (days > 1 ? "s" : "");
        if (days < 30) return "Il y a " + (days / 7) + " semaine" + ((days / 7) > 1 ? "s" : "");
        return "Il y a plus d'un mois";
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "PENDING": return "bg-yellow-100 text-yellow-800";
            case "APPROVED": return "bg-green-100 text-green-800";
            case "DENIED": return "bg-red-100 text-red-800";
            default: return "bg-gray-100 text-gray-800";
        }
    }

    private String getStatusIcon(String status) {
        switch (status) {
            case "PENDING": return "⏳";
            case "APPROVED": return "✅";
            case "DENIED": return "❌";
            default: return "📋";
        }
    }

    private String getTypeLabel(String type) {
        switch (type) {
            case "RENEWAL_ACCEPTANCE": return "Acceptation de renouvellement";
            case "REASSIGNMENT_SUGGESTION": return "Suggestion de réassignation";
            default: return type;
        }
    }
}