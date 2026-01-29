// ProjectRepository.java - Fixed version
package com.example.back.repository;

import com.example.back.entity.Project;
import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Find by code
    Optional<Project> findByCode(String code);
    boolean existsByCode(String code);

    // Find by name
    Optional<Project> findByName(String name);
    boolean existsByName(String name);

    // Find projects by Chef de Projet
    List<Project> findByChefDeProjetId(Long chefDeProjetId);
    List<Project> findByChefDeProjet(User chefDeProjet);

    // Find projects by application
    List<Project> findByApplicationId(Long applicationId);

    // Find projects by status
    List<Project> findByStatus(String status);

    // Find active projects
    List<Project> findByStatusIn(List<String> statuses);

    // Find projects by client name
    List<Project> findByClientNameContainingIgnoreCase(String clientName);

    // Count projects for a Chef de Projet
    Long countByChefDeProjetId(Long chefDeProjetId);

    // Check if project has conventions
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Convention c WHERE c.project.id = :projectId")
    boolean hasConventions(@Param("projectId") Long projectId);

    // Find projects for a specific Chef de Projet with status
    List<Project> findByChefDeProjetIdAndStatus(Long chefDeProjetId, String status);

    // Search projects by multiple criteria
// In ProjectRepository.java
    @Query("SELECT p FROM Project p WHERE " +
            "(:code IS NULL OR p.code LIKE %:code%) AND " +
            "(:name IS NULL OR p.name LIKE %:name%) AND " +
            "(:clientName IS NULL OR p.clientName LIKE %:clientName%) AND " +
            "(:chefDeProjetId IS NULL OR p.chefDeProjet.id = :chefDeProjetId) AND " +
            "(:applicationId IS NULL OR p.application.id = :applicationId) AND " +
            "(:status IS NULL OR p.status = :status)")
    List<Project> searchProjects(@Param("code") String code,
                                 @Param("name") String name,
                                 @Param("clientName") String clientName,
                                 @Param("chefDeProjetId") Long chefDeProjetId,
                                 @Param("applicationId") Long applicationId,
                                 @Param("status") String status);

    // Find projects with conventions count
    @Query("SELECT p, COUNT(c) as conventionCount FROM Project p " +
            "LEFT JOIN Convention c ON c.project = p " +
            "WHERE (:chefDeProjetId IS NULL OR p.chefDeProjet.id = :chefDeProjetId) " +
            "GROUP BY p")
    List<Object[]> findProjectsWithConventionCount(@Param("chefDeProjetId") Long chefDeProjetId);



    // FIXED: Find delayed projects
    @Query("SELECT p FROM Project p WHERE " +
            "p.dateFin IS NOT NULL AND " +
            "p.dateFin < CURRENT_DATE AND " +
            "p.status IN ('PLANIFIE', 'EN_COURS') AND " +
            "p.progress < 100")
    List<Project> findDelayedProjects();



    @Query("SELECT p FROM Project p WHERE p.dateFin IS NOT NULL AND p.dateFin <= :endDate AND p.status = 'EN_COURS'")
    List<Project> findProjectsEndingSoon(@Param("endDate") LocalDate endDate);

    List<Project> findByChefDeProjetIsNull();

    // Or with a custom query if needed
    @Query("SELECT p FROM Project p WHERE p.chefDeProjet IS NULL ORDER BY p.createdAt DESC")
    List<Project> findUnassignedProjects();
}