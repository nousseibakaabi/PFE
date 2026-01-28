// ApplicationRepository.java
package com.example.back.repository;

import com.example.back.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByCode(String code);
    boolean existsByName(String name);
    Optional<Application> findByCode(String code);
}