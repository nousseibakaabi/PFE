package com.example.back.repository;

import com.example.back.entity.Facture;
import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {


    boolean existsByNumeroFacture(String numeroFacture);


    List<Facture> findByStatutPaiement(String statutPaiement);

    List<Facture> findByConventionIdOrderByDateFacturationAsc(Long conventionId);


    @Query("SELECT f FROM Facture f WHERE f.convention.id = :conventionId")
    List<Facture> findByConventionId(@Param("conventionId") Long conventionId);

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


    @Query("SELECT f FROM Facture f WHERE f.convention.createdBy = :user")
    List<Facture> findByConventionCreatedBy(@Param("user") User user);

    @Query("SELECT f FROM Facture f WHERE f.convention.createdBy = :user AND f.statutPaiement = :statut")
    List<Facture> findByConventionCreatedByAndStatutPaiement(@Param("user") User user,
                                                             @Param("statut") String statut);

    @Modifying
    @Query("DELETE FROM Facture f WHERE f.convention.id = :conventionId")
    void deleteByConventionId(@Param("conventionId") Long conventionId);

    List<Facture> findByDateFacturationBetween(LocalDate startDate, LocalDate endDate);

}