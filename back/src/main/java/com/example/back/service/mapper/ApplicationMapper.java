package com.example.back.service.mapper;

import com.example.back.entity.Application;
import com.example.back.entity.Convention;
import com.example.back.payload.response.ApplicationResponse;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ApplicationMapper {

    public ApplicationResponse toResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();

        // Basic fields
        response.setId(application.getId());
        response.setCode(application.getCode());
        response.setName(application.getName());
        response.setDescription(application.getDescription());
        response.setClientName(application.getClientName());
        response.setClientEmail(application.getClientEmail());
        response.setClientPhone(application.getClientPhone());
        response.setDateDebut(application.getDateDebut());
        response.setDateFin(application.getDateFin());
        response.setStatus(application.getStatus());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        response.setMaxUser(application.getMaxUser());
        response.setMinUser(application.getMinUser());


        // Calculated fields
        response.setDaysRemaining(application.getDaysRemaining());
        response.setTotalDays(application.getTotalDays());
        response.setDaysElapsed(application.getDaysElapsed());
        response.setTimeBasedProgress(application.getTimeBasedProgress());
        response.setStatusColor(application.getStatusColor());
        response.setDateRange(application.getDateRange());
        response.setTimeRemainingString(application.getTimeRemainingString());

        // Chef de Projet info
        if (application.getChefDeProjet() != null) {
            response.setChefDeProjetId(application.getChefDeProjet().getId());
            response.setChefDeProjetUsername(application.getChefDeProjet().getUsername());
            response.setChefDeProjetFullName(application.getChefProjetName());
            response.setChefDeProjetEmail(application.getChefDeProjet().getEmail());
        }

        // Conventions
        response.setConventionsCount(application.getConventionsCount());

        if (application.getConventions() != null && !application.getConventions().isEmpty()) {
            // Get recent conventions (last 5)
            response.setRecentConventions(application.getConventions().stream()
                    .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                    .limit(5)
                    .map(this::toConventionMiniResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private ApplicationResponse.ConventionMiniResponse toConventionMiniResponse(Convention convention) {
        ApplicationResponse.ConventionMiniResponse mini = new ApplicationResponse.ConventionMiniResponse();
        mini.setId(convention.getId());
        mini.setReferenceConvention(convention.getReferenceConvention());
        mini.setLibelle(convention.getLibelle());
        mini.setEtat(convention.getEtat());
        return mini;
    }
}