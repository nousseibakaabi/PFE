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


    List<Facture> findByStatutPaiement(String statutPaiement);



    @Query("SELECT SUM(f.montantTTC) FROM Facture f WHERE f.statutPaiement = 'PAYE' AND YEAR(f.datePaiement) = :year AND MONTH(f.datePaiement) = :month")
    BigDecimal findTotalPayeParMois(@Param("year") int year, @Param("month") int month);





    @Query("SELECT f.statutPaiement, COUNT(f) as count FROM Facture f WHERE f.convention.id = :conventionId GROUP BY f.statutPaiement")
    List<Object[]> findInvoiceStatusesByConventionId(@Param("conventionId") Long conventionId);

    // Or use this simpler method
    @Query("SELECT COUNT(f) FROM Facture f WHERE f.convention.id = :conventionId")
    Long countInvoicesByConvention(@Param("conventionId") Long conventionId);

    @Query("SELECT COUNT(f) FROM Facture f WHERE f.convention.id = :conventionId AND f.statutPaiement != 'PAYE'")
    Long countUnpaidInvoicesByConvention(@Param("conventionId") Long conventionId);

    @Query("SELECT f FROM Facture f WHERE f.convention.id = :conventionId")
    List<Facture> findByConventionId(@Param("conventionId") Long conventionId);


    @Query("SELECT f FROM Facture f WHERE f.dateEcheance < :currentDate AND f.statutPaiement = 'NON_PAYE'")
    List<Facture> findOverdueInvoices(@Param("currentDate") LocalDate currentDate);

    // You can keep the existing method too, or rename it
    @Query("SELECT f FROM Facture f WHERE f.dateEcheance < :date AND f.statutPaiement = 'NON_PAYE'")
    List<Facture> findFacturesEnRetard(@Param("date") LocalDate date);


    @Query("SELECT f FROM Facture f WHERE f.dateEcheance = :date")
    List<Facture> findByDateEcheance(@Param("date") LocalDate date);

    @Query("SELECT f FROM Facture f WHERE f.dateEcheance BETWEEN :startDate AND :endDate")
    List<Facture> findByDateEcheanceBetween(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    @Query("SELECT f FROM Facture f WHERE f.dateEcheance BETWEEN :startDate AND :endDate AND f.statutPaiement != :status")
    List<Facture> findByDateEcheanceBetweenAndStatutPaiementNot(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status);

    @Query("SELECT f FROM Facture f WHERE f.dateEcheance BETWEEN :startDate AND :endDate AND f.statutPaiement = :status")
    List<Facture> findByDateEcheanceBetweenAndStatutPaiement(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status);
}