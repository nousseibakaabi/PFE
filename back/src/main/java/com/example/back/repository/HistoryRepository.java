package com.example.back.repository;

import com.example.back.entity.History;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    List<History> findByUserIdOrderByTimestampDesc(Long userId);

    List<History> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);

    @Query("SELECT h FROM History h WHERE DATE(h.timestamp) = :date ORDER BY h.timestamp DESC")
    List<History> findByDate(@Param("date") LocalDate date);

    @Query("SELECT DISTINCT DATE(h.timestamp) FROM History h ORDER BY DATE(h.timestamp) DESC")
    List<LocalDate> findDistinctDates();

    @Query("SELECT h FROM History h ORDER BY h.timestamp DESC")
    List<History> findRecent(Pageable pageable);

    @Query("SELECT h FROM History h WHERE " +
            "(:entityType IS NULL OR h.entityType = :entityType) AND " +
            "(:actionType IS NULL OR h.actionType = :actionType) AND " +
            "(:userId IS NULL OR h.user.id = :userId) AND " +
            "(:date IS NULL OR DATE(h.timestamp) = :date) " +
            "ORDER BY h.timestamp DESC")
    List<History> searchHistory(
            @Param("entityType") String entityType,
            @Param("actionType") String actionType,
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );
}