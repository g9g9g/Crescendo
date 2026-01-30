package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.entity.UserSheet;

import java.util.List;

public interface UserSheetRepositoryCustom {
//
//    /**
//     * 내 악보 검색 (동적 쿼리)
//     * @param userId 사용자 ID
//     * @param searchKeyword 검색어 (제목, 작곡가)
//     * @param instrumentId 악기 ID
//     * @param genreIds 장르 ID 리스트
//     * @param tierCode 티어 코드
//     * @param sortType 정렬 타입 (1: 최신순, 2: 진행률순, 3: 제목순)
//     * @return 내 악보 리스트
//     */
//    List<UserSheet> searchUserSheets(
//            Integer userId,
//            String searchKeyword,
//            Integer instrumentId,
//            List<Integer> genreIds,
//            String tierCode,
//            Integer sortType
//    );

    /**
     * 완주한 곡 조회 (프로필용)
     * - progressRate = 100인 악보
     * - 정렬: tierId 높은 순 → bestScore 높은 순 → 최신순
     * - TOP 50개 제한
     * @param userId 사용자 ID
     * @return 완주한 악보 리스트
     */
    List<UserSheet> findCompletedSheets(Integer userId);
}