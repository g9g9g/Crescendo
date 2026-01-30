package com.d102.crescendo.domain.performance.repository;

import com.d102.crescendo.domain.performance.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerformanceRepository extends JpaRepository<Performance, Integer> {

    @Query("SELECT p FROM Performance p " +
            "JOIN FETCH p.userSheet us " +
            "JOIN FETCH us.sheet s " +
            "JOIN FETCH s.genre g " +
            "JOIN FETCH s.instrument i " +
            "JOIN FETCH s.tier t " +
            "WHERE us.user.userId = :userId " +
            "AND us.deletedYes = false " +
            "ORDER BY p.endedAt DESC")
    List<Performance> findRecentPerformancesByUserId(@Param("userId") Integer userId);

    /**
     * 특정 사용자 악보의 연주 기록 조회 (최신순, 평가 모드만)
     * - practiceMode가 false인 것만 조회
     * - PerformanceEvaluation과 함께 조회
     */
    @Query("SELECT p FROM Performance p " +
            "LEFT JOIN FETCH p.evaluation " +
            "WHERE p.userSheet.userSheetId = :userSheetId " +
            "AND p.practiceMode = false " +
            "ORDER BY p.endedAt DESC")
    List<Performance> findEvaluationModeByUserSheetIdOrderByEndedAtDesc(@Param("userSheetId") Integer userSheetId);


    /**
     * 특정 사용자 악보의 가장 최근 연주 기록 조회 (연습/평가 모드 상관없이)
     * - startBar 계산용
     */
    @Query("SELECT p FROM Performance p " +
            "WHERE p.userSheet.userSheetId = :userSheetId " +
            "ORDER BY p.endedAt DESC " +
            "LIMIT 1")
    Optional<Performance> findLatestByUserSheetId(@Param("userSheetId") Integer userSheetId);

    /**
     * Performance 조회 시 UserSheet를 fetch join (bestScore 업데이트용)
     */
    @Query("SELECT p FROM Performance p " +
            "JOIN FETCH p.userSheet " +
            "WHERE p.playId = :playId")
    Optional<Performance> findByIdWithUserSheet(@Param("playId") Integer playId);

}