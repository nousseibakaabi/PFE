package com.example.back.repository;

import com.example.back.entity.Convention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConventionRepository extends JpaRepository<Convention, Long> {

    Optional<Convention> findByReference(String reference);

    boolean existsByReference(String reference);

    List<Convention> findByStructureId(Long structureId);

    List<Convention> findByGouvernoratId(Long zoneId);

    List<Convention> findByEtat(String etat);

    @Query("SELECT c FROM Convention c WHERE c.dateFin < :date AND c.etat = 'EN_COURS'")
    List<Convention> findConventionsExpirees(@Param("date") LocalDate date);

    @Query("SELECT c FROM Convention c WHERE c.dateDebut <= :date AND c.dateFin >= :date")
    List<Convention> findConventionsEnCours(@Param("date") LocalDate date);
}