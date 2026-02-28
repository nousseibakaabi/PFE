// OldFactureRepository.java
package com.example.back.repository;

import com.example.back.entity.OldConvention;
import com.example.back.entity.OldFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OldFactureRepository extends JpaRepository<OldFacture, Long> {

    List<OldFacture> findByOldConvention(OldConvention oldConvention);
}