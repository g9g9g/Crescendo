package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.performance.entity.Performance;
import com.d102.crescendo.domain.sheet.entity.Genre;
import com.d102.crescendo.domain.sheet.entity.UserSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSheetRepository extends JpaRepository<UserSheet, Integer>, UserSheetRepositoryCustom {

    @Query("SELECT us FROM UserSheet us " +
            "JOIN FETCH us.sheet s " +
            "JOIN FETCH s.genre g " +
            "JOIN FETCH s.instrument i " +
            "JOIN FETCH s.tier t " +
            "WHERE us.user.userId = :userId " +
            "AND us.deletedYes = false " +
            "AND us.lastAccessedAt IS NOT NULL " +
            "ORDER BY us.lastAccessedAt DESC")
    List<UserSheet> findRecentUserSheetsByUserId(@Param("userId") Integer userId);

    /**
     * 내 악보 상세 조회 (fetch join으로 N+1 방지)
     */
    @Query("SELECT us FROM UserSheet us " +
            "JOIN FETCH us.sheet s " +
            "LEFT JOIN FETCH s.genre " +
            "LEFT JOIN FETCH s.tier " +
            "LEFT JOIN FETCH s.instrument " +
            "WHERE us.userSheetId = :userSheetId " +
            "AND us.deletedYes = false")
    Optional<UserSheet> findUserSheetDetailById(@Param("userSheetId") Integer userSheetId);

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
     * 사용자가 특정 악보를 이미 가지고 있는지 확인 (삭제되지 않은 것만)
     */
    @Query("SELECT us FROM UserSheet us " +
            "WHERE us.user.userId = :userId " +
            "AND us.sheet.sheetId = :sheetId " +
            "AND us.deletedYes = false")
    Optional<UserSheet> findByUserIdAndSheetId(@Param("userId") Integer userId, @Param("sheetId") Integer sheetId);


}
