package com.example.back.repository;

import com.example.back.entity.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByNumeroFacture(String numeroFacture);

    boolean existsByNumeroFacture(String numeroFacture);

    List<Facture> findByConventionId(Long conventionId);

    List<Facture> findByStatutPaiement(String statutPaiement);

    @Query("SELECT f FROM Facture f WHERE f.dateEcheance < :date AND f.statutPaiement = 'NON_PAYE'")
    List<Facture> findFacturesEnRetard(@Param("date") LocalDate date);

    @Query("SELECT SUM(f.montantTTC) FROM Facture f WHERE f.statutPaiement = 'PAYE' AND YEAR(f.datePaiement) = :year AND MONTH(f.datePaiement) = :month")
    BigDecimal findTotalPayeParMois(@Param("year") int year, @Param("month") int month);

    @Query("SELECT f FROM Facture f WHERE f.convention.structure.id = :structureId")
    List<Facture> findByStructureId(@Param("structureId") Long structureId);
}