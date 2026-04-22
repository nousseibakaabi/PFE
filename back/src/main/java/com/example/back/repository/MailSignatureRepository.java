package com.example.back.repository;

import com.example.back.entity.MailSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MailSignatureRepository extends JpaRepository<MailSignature, Long> {

}