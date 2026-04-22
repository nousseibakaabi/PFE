package com.example.back.repository;

import com.example.back.entity.User;
import com.example.back.entity.Workload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkloadRepository extends JpaRepository<Workload, Long> {

    Optional<Workload> findByChefDeProjet(User chefDeProjet);

    @Query("SELECT w FROM Workload w ORDER BY w.currentWorkloadScore DESC")
    List<Workload> findAllOrderedByWorkload();

}