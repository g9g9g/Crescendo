package com.d102.crescendo.domain.recommendation.repository;

import com.d102.crescendo.domain.recommendation.dto.UserPlayRecord;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;

import java.util.List;
import java.util.Map;

/**
 * 추천 시스템을 위한 커스텀 Repository 인터페이스
 */
public interface RecommendationRepositoryCustom {

    /**
     * 사용자의 연주 활동 기록 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 활동 기록 리스트
     */
    List<UserPlayRecord> findUserPlayRecords(Integer userId);

    /**
     * 추천 후보 악보 조회 (이미 담은 악보 제외, visible=true만)
     * 난이도 지표도 함께 fetch join
     *
     * @param userId 사용자 ID
     * @return 추천 후보 악보 리스트
     */
    List<SheetMusic> findRecommendationCandidates(Integer userId);

    /**
     * 특정 악보 ID 목록에 대한 SheetMusic 조회 (난이도 지표 포함)
     *
     * @param sheetIds 악보 ID 목록
     * @return sheetId → SheetMusic 맵
     */
    Map<Integer, SheetMusic> findSheetsByIds(List<Integer> sheetIds);

    /**
     * 신규 사용자를 위한 인기 악보 조회 (선호 악기 + 선호 장르 기반)
     * - 사용자의 선호 악기(UserInstrumentTier)와 선호 장르(UserGenre)에 맞는 악보만 조회
     * - 다운로드 수(download_number) 많은 순서로 정렬
     * - 이미 담은 악보 제외
     *
     * @param userId 사용자 ID
     * @param limit 조회할 악보 개수
     * @return 인기 악보 리스트
     */
    List<SheetMusic> findPopularSheetsByUserPreferences(Integer userId, int limit);
}
