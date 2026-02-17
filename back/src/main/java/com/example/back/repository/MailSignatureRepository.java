// MailSignatureRepository.java
package com.example.back.repository;

import com.example.back.entity.MailSignature;
import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MailSignatureRepository extends JpaRepository<MailSignature, Long> {

    List<MailSignature> findByUserOrderByNameAsc(User user);

    Optional<MailSignature> findByUserAndIsDefaultTrue(User user);
}