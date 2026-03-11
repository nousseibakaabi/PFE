package com.example.back.repository;

import com.example.back.entity.Application;
import com.example.back.entity.Convention;
import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConventionRepository extends JpaRepository<Convention, Long> {


    // Find conventions that need status update
    @Query("SELECT c FROM Convention c WHERE c.archived = false AND c.etat IS NOT NULL")
    List<Convention> findAllActiveConventions();


    Optional<Convention> findTopByApplicationAndArchivedFalseOrderByCreatedAtDesc(Application application);

    // Find archived conventions
    List<Convention> findByArchivedTrue();
    List<Convention> findByArchivedFalse();

    // Find expired conventions
    @Query("SELECT c FROM Convention c WHERE c.dateFin < :currentDate AND c.archived = false")
    List<Convention> findExpiredConventions(@Param("currentDate") LocalDate currentDate);

    // Find conventions that are late
    @Query("SELECT c FROM Convention c WHERE c.dateFin < :currentDate AND c.etat != 'TERMINE' AND c.archived = false")
    List<Convention> findLateConventions(@Param("currentDate") LocalDate currentDate);

    // Count methods for deletion validation
    @Query("SELECT COUNT(c) FROM Convention c WHERE c.application.id = :applicationId")
    Long countByApplicationId(@Param("applicationId") Long applicationId);






    @Query("SELECT COUNT(c) FROM Convention c WHERE c.structureResponsable.id = :structureId OR c.structureBeneficiel.id = :structureId")
    Long countByStructureResponsableIdOrStructureBeneficielId(@Param("structureId") Long structureId);

    @Query("SELECT COUNT(c) FROM Convention c WHERE c.structureResponsable.zoneGeographique.id = :zoneId")
    Long countByZoneId(@Param("zoneId") Long zoneId);

    // Remove or replace this method since it doesn't exist anymore
    // List<Convention> findByStructureId(Long structureId);

    // Add new methods to find by structure interne or externe
    List<Convention> findByStructureResponsableId(Long structureResponsableId);
    List<Convention> findByStructureBeneficielId(Long structureBeneficielId);

    // Find conventions by either structure
    @Query("SELECT c FROM Convention c WHERE c.structureResponsable.id = :structureId OR c.structureBeneficiel.id = :structureId")
    List<Convention> findByEitherStructureId(@Param("structureId") Long structureId);



    @Query("SELECT c FROM Convention c WHERE c.etat = :etat AND c.archived = false")
    List<Convention> findByEtat(@Param("etat") String etat);

    // Find conventions that should transition from EN_ATTENTE to EN_COURS
    @Query("SELECT c FROM Convention c WHERE c.dateDebut <= :date AND c.etat = 'EN_ATTENTE' AND c.archived = false")
    List<Convention> findConventionsStartingOnOrBefore(@Param("date") LocalDate date);

    List<Convention> findByApplicationId(Long applicationId);
    List<Convention> findByApplicationIdAndArchivedFalse(Long applicationId);


    // Method to check if a convention belongs to a user
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Convention c WHERE c.id = :conventionId AND c.createdBy = :user")
    boolean existsByIdAndCreatedBy(@Param("conventionId") Long conventionId,
                                   @Param("user") User user);


    List<Convention> findByCreatedByAndArchivedTrue(User createdBy);
    List<Convention> findByCreatedByAndArchivedFalse(User createdBy);
    List<Convention> findByCreatedBy(User createdBy);



    // Add this method to find the highest sequence number for a year
    @Query("SELECT MAX(CAST(SUBSTRING(c.referenceConvention, LENGTH(:prefix) + 1) AS integer)) " +
            "FROM Convention c " +
            "WHERE c.referenceConvention LIKE :prefix%")
    Integer findMaxSequenceNumber(@Param("prefix") String prefix);






    @Query("SELECT CAST(SUBSTRING(c.referenceConvention, LENGTH(:prefix) + 1) AS integer) " +
            "FROM Convention c " +
            "WHERE c.referenceConvention LIKE CONCAT(:prefix, '%') " +
            "ORDER BY CAST(SUBSTRING(c.referenceConvention, LENGTH(:prefix) + 1) AS integer)")
    List<Integer> findUsedSequencesByYear(@Param("prefix") String prefix);

    // Alternative: Get all references for a year
    @Query("SELECT c.referenceConvention FROM Convention c " +
            "WHERE c.referenceConvention LIKE CONCAT('CONV-', :year, '-%') " +
            "ORDER BY c.referenceConvention")
    List<String> findReferencesByYear(@Param("year") String year);

    // Keep the existing methods for backward compatibility
    Boolean existsByReferenceConvention(String reference);


    List<Convention> findByApplicationAndArchivedFalse(Application application);

    // Or if you need a simpler version
    List<Convention> findByApplication(Application application);


    List<Convention> findByApplicationAndArchivedFalseOrderByUpdatedAtDesc(Application application);


    List<Convention> findByApplicationAndArchivedTrue(Application application);

    @Query("SELECT c FROM Convention c WHERE c.application.id = :applicationId AND c.archived = true")
    List<Convention> findByApplicationIdAndArchivedTrue(@Param("applicationId") Long applicationId);

}