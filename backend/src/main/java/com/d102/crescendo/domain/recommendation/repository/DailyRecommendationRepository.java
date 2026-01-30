package com.d102.crescendo.domain.recommendation.repository;

import com.d102.crescendo.domain.recommendation.entity.DailyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyRecommendationRepository
        extends JpaRepository<DailyRecommendation, Long> {

    List<DailyRecommendation> findByUserIdAndRecDateOrderByRankAsc(
            Integer userId,
            LocalDate recDate
    );

    /**
     * 특정 악보를 추천받은 모든 사용자 ID 조회
     */
    @Query("SELECT DISTINCT dr.userId FROM DailyRecommendation dr WHERE dr.sheetId = :sheetId")
    List<Integer> findUserIdsBySheetId(@Param("sheetId") Integer sheetId);

    /**
     * 특정 사용자의 오늘의 추천 전체 삭제
     */
    @Modifying
    @Query("DELETE FROM DailyRecommendation dr WHERE dr.userId = :userId AND dr.recDate = :recDate")
    void deleteByUserIdAndRecDate(@Param("userId") Integer userId, @Param("recDate") LocalDate recDate);
}
