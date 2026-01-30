package com.d102.crescendo.domain.recommendation.repository;

import com.d102.crescendo.domain.recommendation.dto.UserPlayRecord;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 추천 시스템을 위한 커스텀 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class RecommendationRepositoryImpl implements RecommendationRepositoryCustom {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<UserPlayRecord> findUserPlayRecords(Integer userId) {
        String jpql = """
                SELECT new com.d102.crescendo.domain.recommendation.dto.UserPlayRecord(
                    us.sheet.sheetId,
                    us.practiceTime,
                    us.progressRate,
                    us.lastAccessedAt
                )
                FROM UserSheet us
                WHERE us.user.userId = :userId
                  AND us.deletedYes = false
                ORDER BY us.lastAccessedAt DESC
                """;

        return entityManager.createQuery(jpql, UserPlayRecord.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public List<SheetMusic> findRecommendationCandidates(Integer userId) {
        String jpql = """
                SELECT DISTINCT s
                FROM SheetMusic s
                LEFT JOIN FETCH s.genre
                LEFT JOIN FETCH s.instrument
                LEFT JOIN FETCH s.tier
                LEFT JOIN FETCH s.difficultyMetric
                WHERE s.visibleYes = true
                  AND s.sourceType = 'SYSTEM'
                  AND s.embedding IS NOT NULL
                  AND s.instrument.instrumentId IN (
                      SELECT uit.instrument.instrumentId
                      FROM UserInstrumentTier uit
                      WHERE uit.user.userId = :userId
                  )
                  AND s.sheetId NOT IN (
                      SELECT us.sheet.sheetId
                      FROM UserSheet us
                      WHERE us.user.userId = :userId
                        AND us.deletedYes = false
                  )
                """;

        return entityManager.createQuery(jpql, SheetMusic.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public Map<Integer, SheetMusic> findSheetsByIds(List<Integer> sheetIds) {
        if (sheetIds == null || sheetIds.isEmpty()) {
            return Map.of();
        }

        String jpql = """
                SELECT DISTINCT s
                FROM SheetMusic s
                LEFT JOIN FETCH s.genre
                LEFT JOIN FETCH s.instrument
                LEFT JOIN FETCH s.tier
                LEFT JOIN FETCH s.difficultyMetric
                WHERE s.sheetId IN :sheetIds
                """;

        List<SheetMusic> sheets = entityManager.createQuery(jpql, SheetMusic.class)
                .setParameter("sheetIds", sheetIds)
                .getResultList();

        return sheets.stream()
                .collect(Collectors.toMap(SheetMusic::getSheetId, sheet -> sheet));
    }

    @Override
    public List<SheetMusic> findPopularSheetsByUserPreferences(Integer userId, int limit) {
        String jpql = """
                SELECT DISTINCT s
                FROM SheetMusic s
                LEFT JOIN FETCH s.genre
                LEFT JOIN FETCH s.instrument
                LEFT JOIN FETCH s.tier
                LEFT JOIN FETCH s.difficultyMetric
                WHERE s.visibleYes = true
                  AND s.sourceType = 'SYSTEM'
                  AND s.instrument.instrumentId IN (
                      SELECT uit.instrument.instrumentId
                      FROM UserInstrumentTier uit
                      WHERE uit.user.userId = :userId
                  )
                  AND s.genre.genreId IN (
                      SELECT ug.genre.genreId
                      FROM UserGenre ug
                      WHERE ug.user.userId = :userId
                  )
                  AND s.sheetId NOT IN (
                      SELECT us.sheet.sheetId
                      FROM UserSheet us
                      WHERE us.user.userId = :userId
                        AND us.deletedYes = false
                  )
                ORDER BY s.downloadNumber DESC
                """;

        return entityManager.createQuery(jpql, SheetMusic.class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
    }
}
