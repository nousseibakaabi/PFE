// MailDraftRepository.java
package com.example.back.repository;

import com.example.back.entity.MailDraft;
import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MailDraftRepository extends JpaRepository<MailDraft, Long> {

    List<MailDraft> findByUserOrderByLastSavedAtDesc(User user);

    Optional<MailDraft> findByUserAndIsSendingFalse(User user);

    long countByUser(User user);
}