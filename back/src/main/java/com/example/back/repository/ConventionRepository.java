package com.example.back.repository;

import com.example.back.entity.Application;
import com.example.back.entity.Convention;
import com.example.back.entity.Structure;
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


    List<Convention> findByStructureBeneficiel(Structure structure);
    List<Convention> findByStructureBeneficielAndArchivedFalse(Structure structure);

    @Query("SELECT c FROM Convention c WHERE c.structureBeneficiel.id = :structureId")
    List<Convention> findByStructureBeneficielId(@Param("structureId") Long structureId);

    Optional<Convention> findTopByApplicationAndArchivedFalseOrderByCreatedAtDesc(Application application);

    List<Convention> findByArchivedTrue();
    List<Convention> findByArchivedFalse();


    @Query("SELECT COUNT(c) FROM Convention c WHERE c.structureResponsable.id = :structureId OR c.structureBeneficiel.id = :structureId")
    Long countByStructureResponsableIdOrStructureBeneficielId(@Param("structureId") Long structureId);

    @Query("SELECT COUNT(c) FROM Convention c WHERE c.structureResponsable.zoneGeographique.id = :zoneId")
    Long countByZoneId(@Param("zoneId") Long zoneId);


    @Query("SELECT c FROM Convention c WHERE c.etat = :etat AND c.archived = false")
    List<Convention> findByEtat(@Param("etat") String etat);

    List<Convention> findByApplicationId(Long applicationId);

    List<Convention> findByCreatedByAndArchivedTrue(User createdBy);
    List<Convention> findByCreatedByAndArchivedFalse(User createdBy);


    @Query("SELECT CAST(SUBSTRING(c.referenceConvention, LENGTH(:prefix) + 1) AS integer) " +
            "FROM Convention c " +
            "WHERE c.referenceConvention LIKE CONCAT(:prefix, '%') " +
            "ORDER BY CAST(SUBSTRING(c.referenceConvention, LENGTH(:prefix) + 1) AS integer)")
    List<Integer> findUsedSequencesByYear(@Param("prefix") String prefix);


    Boolean existsByReferenceConvention(String reference);


    List<Convention> findByApplicationAndArchivedFalse(Application application);

    List<Convention> findByApplication(Application application);

    List<Convention> findByApplicationAndArchivedFalseOrderByUpdatedAtDesc(Application application);

    List<Convention> findByDateDebutBetween(LocalDate startDate, LocalDate endDate);



}