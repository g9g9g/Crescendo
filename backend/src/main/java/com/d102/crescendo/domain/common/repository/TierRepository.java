package com.d102.crescendo.domain.common.repository;

import com.d102.crescendo.domain.common.entity.Tier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TierRepository extends JpaRepository<Tier, Integer> {
    Optional<Tier> findByTierId(Integer tierId);

    /**
     * 티어 레벨 순으로 정렬된 모든 티어 조회
     */
    @Query("SELECT t FROM Tier t ORDER BY t.tierLevel ASC")
    List<Tier> findAllOrderByTierLevel();

    /**
     * 특정 티어 레벨보다 높은 가장 낮은 티어 조회 (레벨업용)
     */
    @Query("SELECT t FROM Tier t WHERE t.tierLevel > :currentLevel ORDER BY t.tierLevel ASC LIMIT 1")
    Optional<Tier> findNextTier(@Param("currentLevel") Short currentLevel);
}