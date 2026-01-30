package com.d102.crescendo.domain.common.repository;

import com.d102.crescendo.domain.common.entity.ExpLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpLogRepository extends JpaRepository<ExpLog, Integer> {

    /**
     * 특정 유저가 특정 악보로 이미 경험치를 받았는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
           "FROM ExpLog e " +
           "WHERE e.user.userId = :userId AND e.sheet.sheetId = :sheetId")
    boolean existsByUserIdAndSheetId(@Param("userId") Integer userId, @Param("sheetId") Integer sheetId);
}