// ZoneGeographiqueRepository.java
package com.example.back.repository;

import com.example.back.entity.ZoneGeographique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ZoneGeographiqueRepository extends JpaRepository<ZoneGeographique, Long> {
    Optional<ZoneGeographique> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByName(String name);
}