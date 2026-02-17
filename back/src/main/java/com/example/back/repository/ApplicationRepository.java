// ApplicationRepository.java
package com.example.back.repository;

import com.example.back.entity.Application;
import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByCode(String code);
    boolean existsByName(String name);
    Optional<Application> findByCode(String code);



    // Find by name
    Optional<Application> findByName(String name);

    // Find applications by Chef de Projet
    List<Application> findByChefDeProjet(User chefDeProjet);

    // Find applications by status
    List<Application> findByStatus(String status);

    // Find active applications
    List<Application> findByStatusIn(List<String> statuses);

    // Find applications by client name
    List<Application> findByClientNameContainingIgnoreCase(String clientName);

    // Count applications for a Chef de Projet
    Long countByChefDeProjetId(Long chefDeProjetId);

    // Check if application has conventions
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Convention c WHERE c.application.id = :applicationId")
    boolean hasConventions(@Param("applicationId") Long applicationId);

    // Find applications for a specific Chef de Projet with status
    List<Application> findByChefDeProjetIdAndStatus(Long chefDeProjetId, String status);

    // Search applications by multiple criteria
    @Query("SELECT a FROM Application a WHERE " +
            "(:code IS NULL OR a.code LIKE %:code%) AND " +
            "(:name IS NULL OR a.name LIKE %:name%) AND " +
            "(:clientName IS NULL OR a.clientName LIKE %:clientName%) AND " +
            "(:chefDeProjetId IS NULL OR a.chefDeProjet.id = :chefDeProjetId) AND " +
            "(:status IS NULL OR a.status = :status)")
    List<Application> searchApplications(@Param("code") String code,
                                         @Param("name") String name,
                                         @Param("clientName") String clientName,
                                         @Param("chefDeProjetId") Long chefDeProjetId,
                                         @Param("status") String status);

    // Find applications with conventions count
    @Query("SELECT a, COUNT(c) as conventionCount FROM Application a " +
            "LEFT JOIN Convention c ON c.application = a " +
            "WHERE (:chefDeProjetId IS NULL OR a.chefDeProjet.id = :chefDeProjetId) " +
            "GROUP BY a")
    List<Object[]> findApplicationsWithConventionCount(@Param("chefDeProjetId") Long chefDeProjetId);

    // Find delayed applications
    @Query("SELECT a FROM Application a WHERE " +
            "a.dateFin IS NOT NULL AND " +
            "a.dateFin < CURRENT_DATE AND " +
            "a.status IN ('PLANIFIE', 'EN_COURS')")
    List<Application> findDelayedApplications();

    List<Application> findByChefDeProjetIsNull();

    // Or with a custom query if needed
    @Query("SELECT a FROM Application a WHERE a.chefDeProjet IS NULL ORDER BY a.createdAt DESC")
    List<Application> findUnassignedApplications();

    // Find applications ending soon
    @Query("SELECT a FROM Application a WHERE a.dateFin IS NOT NULL AND a.dateFin <= :endDate AND a.status = 'EN_COURS'")
    List<Application> findApplicationsEndingSoon(@Param("endDate") LocalDate endDate);

    List<Application> findByChefDeProjetId(Long chefDeProjetId);

    // Find applications with conventions created by specific user
    @Query("SELECT DISTINCT a FROM Application a " +
            "JOIN a.conventions c " +
            "WHERE c.createdBy.id = :userId")
    List<Application> findApplicationsWithConventionsByUser(@Param("userId") Long userId);

    @Query("SELECT a.code FROM Application a WHERE a.code LIKE CONCAT('APP-', :year, '-%') ORDER BY a.code")
    List<String> findApplicationCodesByYear(@Param("year") String year);

    // ApplicationRepository.java
    @Query("SELECT SUBSTRING(a.code, LENGTH(CONCAT('APP-', :year, '-')) + 1) " +
            "FROM Application a " +
            "WHERE a.code LIKE CONCAT('APP-', :year, '-%')")
    List<Integer> findUsedSequencesByYear(@Param("year") String year);


    /**
     * Find all applications that have no conventions
     */
    @Query("SELECT a FROM Application a WHERE NOT EXISTS (SELECT c FROM Convention c WHERE c.application = a AND c.archived = false)")
    List<Application> findApplicationsWithoutConventions();
}