package com.d102.crescendo.domain.recommendation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 유저 한 명에 대한 "오늘의 악보" 후보 TOP N을 CBF 기반으로 뽑는 Repository
 */
//C
// 65BF 로직(장르/완주티어/인기도/소유악보 보너스)
@Repository
@RequiredArgsConstructor
public class TodaySheetRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<TodaySheetCandidate> findTopTodaySheetsForUser(Integer userId, int limit) {
        String sql = """
            WITH
            -- 1) 유저의 장르별 보유 악보 수
            user_genre AS (
                SELECT sm.genre_id,
                       COUNT(*) AS genre_count
                FROM user_sheet us
                JOIN sheet_music sm ON us.sheet_id = sm.sheet_id
                WHERE us.user_id = ?
                  AND us.deleted_yes = FALSE
                GROUP BY sm.genre_id
            ),
            user_genre_max AS (
                SELECT MAX(genre_count) AS max_genre_count FROM user_genre
            ),
            -- 2) 유저가 완주(progress_rate=100)한 악보들의 평균 티어 레벨
            user_completed_tier AS (
                SELECT AVG(t.tier_level) AS avg_completed_tier_level
                FROM user_sheet us
                JOIN sheet_music sm ON us.sheet_id = sm.sheet_id
                JOIN tier t ON sm.tier_id = t.tier_id
                WHERE us.user_id = ?
                  AND us.deleted_yes = FALSE
                  AND us.progress_rate = 100
                  AND sm.source_type = 'system'
                  AND sm.tier_id IS NOT NULL
            ),
            -- 3) 전체 악보 중 최대 다운로드 수 (인기도 정규화를 위해)
            global_download_max AS (
                SELECT MAX(download_number) AS max_download_number
                FROM sheet_music
                WHERE visible_yes = TRUE
            )
            SELECT
                sm.sheet_id,

                -- 장르 점수 (0~1)
                COALESCE(ug.genre_count / ugmax.max_genre_count::float, 0) AS genre_score,

                -- 난이도(티어) 매칭 점수 (완주 평균 티어와의 차이, 0~1)
                CASE
                    WHEN t.tier_level IS NULL OR uct.avg_completed_tier_level IS NULL THEN 0
                    ELSE 1 - LEAST(ABS(t.tier_level - uct.avg_completed_tier_level) / 3.0, 1.0)
                END AS tier_match_score,

                -- 인기도 점수 (0~1)
                COALESCE(sm.download_number / NULLIF(gdm.max_download_number, 0)::float, 0) AS popularity_score,

                -- 내가 소유한 악보인지 여부 (보너스용)
                CASE WHEN us.user_id IS NOT NULL THEN 1 ELSE 0 END AS own_sheet_bonus,

                -- 최종 점수
                (
                    0.35 * COALESCE(ug.genre_count / ugmax.max_genre_count::float, 0)          -- 라이브러리 장르 선호
                  + 0.30 * CASE
                                WHEN t.tier_level IS NULL OR uct.avg_completed_tier_level IS NULL THEN 0
                                ELSE 1 - LEAST(ABS(t.tier_level - uct.avg_completed_tier_level) / 3.0, 1.0)
                           END                                                               -- 완주 난이도 매칭
                  + 0.25 * COALESCE(sm.download_number / NULLIF(gdm.max_download_number, 0)::float, 0) -- 인기도
                  + 0.10 * CASE WHEN us.user_id IS NOT NULL THEN 1 ELSE 0 END              -- 소유 악보 보너스
                ) AS final_score

            FROM sheet_music sm
            -- 유저가 소유한 악보인지 체크
            LEFT JOIN user_sheet us
                ON us.sheet_id = sm.sheet_id
               AND us.user_id = ?
               AND us.deleted_yes = FALSE
            LEFT JOIN user_genre ug
                ON ug.genre_id = sm.genre_id
            LEFT JOIN user_genre_max ugmax ON 1=1
            LEFT JOIN user_completed_tier uct ON 1=1
            LEFT JOIN tier t ON sm.tier_id = t.tier_id
            LEFT JOIN global_download_max gdm ON 1=1
            WHERE sm.visible_yes = TRUE
            ORDER BY final_score DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setInt(1, userId); // user_genre
                    ps.setInt(2, userId); // user_completed_tier
                    ps.setInt(3, userId); // user_sheet JOIN (own_sheet_bonus)
                    ps.setInt(4, limit);  // LIMIT
                },
                (rs, rowNum) -> new TodaySheetCandidate(
                        rs.getInt("sheet_id"),
                        rs.getDouble("final_score")
                )
        );
    }
}