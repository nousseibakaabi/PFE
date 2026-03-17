package com.example.back.repository;

import com.example.back.entity.Convention;
import com.example.back.entity.OldConvention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface OldConventionRepository extends JpaRepository<OldConvention, Long> {

    List<OldConvention> findByCurrentConventionOrderByRenewalVersionDesc(Convention currentConvention);

    @Query("SELECT MAX(oc.renewalVersion) FROM OldConvention oc WHERE oc.currentConvention = :convention")
    Integer findMaxRenewalVersion(@Param("convention") Convention convention);

    
    // ADD THIS METHOD - with Pageable support
    Page<OldConvention> findByCurrentConventionOrderByRenewalVersionDesc(Convention currentConvention, Pageable pageable);

    
}