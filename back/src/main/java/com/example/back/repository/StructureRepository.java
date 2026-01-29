// StructureRepository.java
package com.example.back.repository;

import com.example.back.entity.Structure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StructureRepository extends JpaRepository<Structure, Long> {
    Optional<Structure> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByName(String name);

    Optional<Structure> findByName(String clientName);
}