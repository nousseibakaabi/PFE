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
    List<Application> findByChefDeProjetAndArchivedTrue(@Param("chef") User chef);

    @Query("SELECT a FROM Application a WHERE a.archived = true")
    List<Application> findByArchivedTrue();

    List<Application> findByChefDeProjet(User chefDeProjet);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Convention c WHERE c.application.id = :applicationId")
    boolean hasConventions(@Param("applicationId") Long applicationId);

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



    List<Application> findByChefDeProjetIsNull();

    @Query("SELECT a FROM Application a WHERE a.dateFin IS NOT NULL AND a.dateFin <= :endDate AND a.status = 'EN_COURS'")
    List<Application> findApplicationsEndingSoon(@Param("endDate") LocalDate endDate);

    List<Application> findByChefDeProjetId(Long chefDeProjetId);

    @Query("SELECT SUBSTRING(a.code, LENGTH(CONCAT('APP-', :year, '-')) + 1) " +
            "FROM Application a " +
            "WHERE a.code LIKE CONCAT('APP-', :year, '-%')")
    List<Integer> findUsedSequencesByYear(@Param("year") String year);


    List<Application> findByClientName(String clientName);

    @Query("SELECT a FROM Application a WHERE NOT EXISTS (SELECT c FROM Convention c WHERE c.application = a AND c.archived = false)")
    List<Application> findApplicationsWithoutConventions();

    @Query("SELECT a FROM Application a WHERE a.archived = false")
    List<Application> findByArchivedFalse();

    @Query("SELECT a FROM Application a WHERE a.chefDeProjet = :chef AND a.archived = false")
    List<Application> findByChefDeProjetAndArchivedFalse(@Param("chef") User chef);


}