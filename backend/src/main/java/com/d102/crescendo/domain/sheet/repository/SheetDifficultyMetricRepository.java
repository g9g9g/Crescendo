package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.entity.SheetDifficultyMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SheetDifficultyMetricRepository extends JpaRepository<SheetDifficultyMetric, Long> {
    Optional<SheetDifficultyMetric> findBySheetSheetId(Integer sheetId);
    void deleteBySheetSheetId(Integer sheetId);
}