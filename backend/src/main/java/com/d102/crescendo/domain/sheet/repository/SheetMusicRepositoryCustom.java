package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.entity.SheetMusic;

import java.util.List;

public interface SheetMusicRepositoryCustom {
    List<Integer> findSimilarSheetsByEmbedding(Integer querySheetId, Integer limit);

    /**
     * 서비스 악보 검색 (동적 쿼리)
     * @param searchKeyword 검색어 (제목, 작곡가)
     * @param instrumentId 악기 ID
     * @param genreIds 장르 ID 리스트
     * @param tierCode 티어 코드
     * @param sortType 정렬 타입 (1: 최신순, 2: 다운로드순, 3: 제목순)
     * @return 서비스 악보 리스트
     */
    List<SheetMusic> searchServiceSheets(
            String searchKeyword,
            Integer instrumentId,
            List<Integer> genreIds,
            String tierCode,
            Integer sortType
    );
}