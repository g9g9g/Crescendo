package com.d102.crescendo.domain.performance.repository;

import com.d102.crescendo.domain.performance.entity.PerformanceEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PerformanceEvaluationRepository extends JpaRepository<PerformanceEvaluation, Integer> {

    @Query("SELECT pe FROM PerformanceEvaluation pe WHERE pe.performance.playId = :playId")
    Optional<PerformanceEvaluation> findByPlayId(@Param("playId") Integer playId);
}