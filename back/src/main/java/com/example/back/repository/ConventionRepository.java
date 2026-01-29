package com.example.back.repository;

import com.example.back.entity.Convention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConventionRepository extends JpaRepository<Convention, Long> {

    boolean existsByReferenceConvention(String referenceConvention);
    boolean existsByReferenceERP(String referenceERP);

    // Find conventions that need status update
    @Query("SELECT c FROM Convention c WHERE c.archived = false AND c.etat IS NOT NULL")
    List<Convention> findAllActiveConventions();



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
    @Query("SELECT COUNT(c) FROM Convention c WHERE c.project.id = :projectId")
    Long countByProjectId(@Param("projectId") Long projectId);






    @Query("SELECT COUNT(c) FROM Convention c WHERE c.structureInterne.id = :structureId OR c.structureExterne.id = :structureId")
    Long countByStructureInterneIdOrStructureExterneId(@Param("structureId") Long structureId);

    @Query("SELECT COUNT(c) FROM Convention c WHERE c.zone.id = :zoneId")
    Long countByZoneId(@Param("zoneId") Long zoneId);

    // Remove or replace this method since it doesn't exist anymore
    // List<Convention> findByStructureId(Long structureId);

    // Add new methods to find by structure interne or externe
    List<Convention> findByStructureInterneId(Long structureInterneId);
    List<Convention> findByStructureExterneId(Long structureExterneId);

    // Find conventions by either structure
    @Query("SELECT c FROM Convention c WHERE c.structureInterne.id = :structureId OR c.structureExterne.id = :structureId")
    List<Convention> findByEitherStructureId(@Param("structureId") Long structureId);



    @Query("SELECT c FROM Convention c WHERE c.etat = :etat AND c.archived = false")
    List<Convention> findByEtat(@Param("etat") String etat);

    // Find conventions that should transition from EN_ATTENTE to EN_COURS
    @Query("SELECT c FROM Convention c WHERE c.dateDebut <= :date AND c.etat = 'EN_ATTENTE' AND c.archived = false")
    List<Convention> findConventionsStartingOnOrBefore(@Param("date") LocalDate date);

    List<Convention> findByProjectId(Long projectId);
    List<Convention> findByProjectIdAndArchivedFalse(Long projectId);


}