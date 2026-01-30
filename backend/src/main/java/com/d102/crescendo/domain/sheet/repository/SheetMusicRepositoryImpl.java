package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class SheetMusicRepositoryImpl implements SheetMusicRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Integer> findSimilarSheetsByEmbedding(Integer querySheetId, Integer limit) {
        // Native query는 SheetMusicRepository에 구현되어 있음
        return new ArrayList<>();
    }

    @Override
    public List<SheetMusic> searchServiceSheets(
            String searchKeyword,
            Integer instrumentId,
            List<Integer> genreIds,
            String tierCode,
            Integer sortType
    ) {
        log.info("서비스 악보 검색 - keyword: {}, instrumentId: {}, genreIds: {}, tierCode: {}, sortType: {}",
                searchKeyword, instrumentId, genreIds, tierCode, sortType);

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT DISTINCT s FROM SheetMusic s ");
        jpql.append("LEFT JOIN FETCH s.genre ");
        jpql.append("LEFT JOIN FETCH s.tier ");
        jpql.append("LEFT JOIN FETCH s.instrument ");
        jpql.append("WHERE s.sourceType = 'SYSTEM' ");
        jpql.append("AND s.visibleYes = true ");

        List<String> conditions = new ArrayList<>();

        // 검색어 조건 (제목 또는 작곡가)
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            conditions.add("(LOWER(s.title) LIKE LOWER(:keyword) OR LOWER(s.composer) LIKE LOWER(:keyword))");
        }

        // 악기 조건
        if (instrumentId != null) {
            conditions.add("s.instrument.instrumentId = :instrumentId");
        }

        // 장르 조건 (복수 선택 가능)
        if (genreIds != null && !genreIds.isEmpty()) {
            conditions.add("s.genre.genreId IN :genreIds");
        }

        // 티어 조건
        if (tierCode != null && !tierCode.trim().isEmpty()) {
            conditions.add("s.tier.tierCode = :tierCode");
        }

        // 조건 추가
        if (!conditions.isEmpty()) {
            jpql.append("AND ").append(String.join(" AND ", conditions)).append(" ");
        }

        // 정렬 조건
        if (sortType == null || sortType == 1) {
            jpql.append("ORDER BY s.updatedAt DESC");
        } else if (sortType == 2) {
            jpql.append("ORDER BY s.downloadNumber DESC, s.updatedAt DESC");
        } else if (sortType == 3) {
            jpql.append("ORDER BY s.title ASC");
        } else {
            jpql.append("ORDER BY s.updatedAt DESC"); // 기본값
        }

        log.debug("Generated JPQL: {}", jpql);

        TypedQuery<SheetMusic> query = entityManager.createQuery(jpql.toString(), SheetMusic.class);

        // 파라미터 바인딩
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            query.setParameter("keyword", "%" + searchKeyword.trim() + "%");
        }

        if (instrumentId != null) {
            query.setParameter("instrumentId", instrumentId);
        }

        if (genreIds != null && !genreIds.isEmpty()) {
            query.setParameter("genreIds", genreIds);
        }

        if (tierCode != null && !tierCode.trim().isEmpty()) {
            query.setParameter("tierCode", tierCode);
        }

        List<SheetMusic> results = query.getResultList();
        log.info("검색 결과: {}건", results.size());

        return results;
    }
}
