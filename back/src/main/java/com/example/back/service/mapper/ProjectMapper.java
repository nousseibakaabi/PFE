// ProjectMapper.java
package com.example.back.service.mapper;

import com.example.back.entity.Project;
import com.example.back.entity.Convention;
import com.example.back.payload.response.ProjectResponse;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();

        // Basic fields
        response.setId(project.getId());
        response.setCode(project.getCode());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setClientName(project.getClientName());
        response.setClientEmail(project.getClientEmail());
        response.setClientPhone(project.getClientPhone());
        response.setClientAddress(project.getClientAddress());
        response.setDateDebut(project.getDateDebut());
        response.setDateFin(project.getDateFin());
        response.setProgress(project.getProgress());
        response.setBudget(project.getBudget());
        response.setStatus(project.getStatus());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        // Calculated fields
        response.setDaysRemaining(project.getDaysRemaining());
        response.setTotalDays(project.getTotalDays());
        response.setDaysElapsed(project.getDaysElapsed());
        response.setTimeBasedProgress(project.getTimeBasedProgress());
        response.setIsDelayed(project.isDelayed());
        response.setStatusColor(project.getStatusColor());
        response.setProgressColor(project.getProgressColor());
        response.setDateRange(project.getDateRange());
        response.setTimeRemainingString(project.getTimeRemainingString());

        // Application info
        if (project.getApplication() != null) {
            response.setApplicationId(project.getApplication().getId());
            response.setApplicationName(project.getApplication().getName());
            response.setApplicationCode(project.getApplication().getCode());
        }

        // Chef de Projet info
        if (project.getChefDeProjet() != null) {
            response.setChefDeProjetId(project.getChefDeProjet().getId());
            response.setChefDeProjetUsername(project.getChefDeProjet().getUsername());
            response.setChefDeProjetFullName(project.getChefProjetName());
            response.setChefDeProjetEmail(project.getChefDeProjet().getEmail());
        }

        // Conventions
        response.setConventionsCount(project.getConventionsCount());

        if (project.getConventions() != null && !project.getConventions().isEmpty()) {
            // Get recent conventions (last 5)
            response.setRecentConventions(project.getConventions().stream()
                    .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                    .limit(5)
                    .map(this::toConventionMiniResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private ProjectResponse.ConventionMiniResponse toConventionMiniResponse(Convention convention) {
        ProjectResponse.ConventionMiniResponse mini = new ProjectResponse.ConventionMiniResponse();
        mini.setId(convention.getId());
        mini.setReferenceConvention(convention.getReferenceConvention());
        mini.setLibelle(convention.getLibelle());
        mini.setEtat(convention.getEtat());
        return mini;
    }
}