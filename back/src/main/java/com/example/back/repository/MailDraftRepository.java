package com.example.back.repository;

import com.example.back.entity.MailDraft;
import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailDraftRepository extends JpaRepository<MailDraft, Long> {

    List<MailDraft> findByUserOrderByLastSavedAtDesc(User user);

    long countByUser(User user);
}