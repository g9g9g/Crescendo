package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.document.UserSheetDocument;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

public interface UserSheetDocumentRepositoryCustom {

    /**
     * 내 악보 검색 (Elasticsearch)
     * @param userId 사용자 ID
     * @param searchKeyword 검색어 (제목, 작곡가, 악기, 장르)
     * @param instrumentId 악기 ID (필터링용)
     * @param genreIds 장르 ID 리스트 (필터링용)
     * @param tierCode 티어 코드 (필터링용)
     * @param minTierLevel 최소 티어 레벨 (범위 검색용)
     * @param maxTierLevel 최대 티어 레벨 (범위 검색용)
     * @param sortType 정렬 타입 (1: 최신순, 2: 진행률순, 3: 제목순)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 검색 결과 (하이라이팅 포함)
     */
    SearchHits<UserSheetDocument> searchUserSheets(
            Integer userId,
            String searchKeyword,
            Integer instrumentId,
            List<Integer> genreIds,
            String tierCode,
            Integer minTierLevel,
            Integer maxTierLevel,
            Integer sortType,
            Integer page,
            Integer size
    );

    /**
     * 내 악보 자동완성 (제목 또는 작곡가)
     * @param userId 사용자 ID
     * @param keyword 검색어
     * @param size 결과 개수 (기본 5개)
     * @return 자동완성 검색 결과
     */
    SearchHits<UserSheetDocument> autocompleteUserSheets(
            Integer userId,
            String keyword,
            Integer size
    );
}