package com.example.back.repository;

import com.example.back.entity.TrainingData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingDataRepository extends JpaRepository<TrainingData, Long> {

    List<TrainingData> findBySynthetic(boolean isSynthetic);

}