package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.entity.UserSheet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class UserSheetRepositoryImpl implements UserSheetRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

//    @Override
//    public List<UserSheet> searchUserSheets(
//            Integer userId,
//            String searchKeyword,
//            Integer instrumentId,
//            List<Integer> genreIds,
//            String tierCode,
//            Integer sortType
//    ) {
//        log.info("내 악보 검색 - userId: {}, keyword: {}, instrumentId: {}, genreIds: {}, tierCode: {}, sortType: {}",
//                userId, searchKeyword, instrumentId, genreIds, tierCode, sortType);
//
//        StringBuilder jpql = new StringBuilder();
//        jpql.append("SELECT DISTINCT us FROM UserSheet us ");
//        jpql.append("JOIN FETCH us.sheet s ");
//        jpql.append("LEFT JOIN FETCH s.genre ");
//        jpql.append("LEFT JOIN FETCH s.tier ");
//        jpql.append("LEFT JOIN FETCH s.instrument ");
//        jpql.append("WHERE us.user.userId = :userId ");
//        jpql.append("AND us.deletedYes = false ");
//
//        List<String> conditions = new ArrayList<>();
//
//        // 검색어 조건 (제목 또는 작곡가)
//        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
//            conditions.add("(LOWER(s.title) LIKE LOWER(:keyword) OR LOWER(s.composer) LIKE LOWER(:keyword))");
//        }
//
//        // 악기 조건
//        if (instrumentId != null) {
//            conditions.add("s.instrument.instrumentId = :instrumentId");
//        }
//
//        // 장르 조건 (복수 선택 가능)
//        if (genreIds != null && !genreIds.isEmpty()) {
//            conditions.add("s.genre.genreId IN :genreIds");
//        }
//
//        // 티어 조건
//        if (tierCode != null && !tierCode.trim().isEmpty()) {
//            conditions.add("s.tier.tierCode = :tierCode");
//        }
//
//        // 조건 추가
//        if (!conditions.isEmpty()) {
//            jpql.append("AND ").append(String.join(" AND ", conditions)).append(" ");
//        }
//
//        // 정렬 조건
//        if (sortType == null || sortType == 1) {
//            jpql.append("ORDER BY us.lastAccessedAt DESC");
//        } else if (sortType == 2) {
//            jpql.append("ORDER BY us.progressRate DESC, us.lastAccessedAt DESC");
//        } else if (sortType == 3) {
//            jpql.append("ORDER BY s.title ASC");
//        } else {
//            jpql.append("ORDER BY us.lastAccessedAt DESC"); // 기본값
//        }
//
//        log.debug("Generated JPQL: {}", jpql);
//
//        TypedQuery<UserSheet> query = entityManager.createQuery(jpql.toString(), UserSheet.class);
//
//        // 파라미터 바인딩
//        query.setParameter("userId", userId);
//
//        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
//            query.setParameter("keyword", "%" + searchKeyword.trim() + "%");
//        }
//
//        if (instrumentId != null) {
//            query.setParameter("instrumentId", instrumentId);
//        }
//
//        if (genreIds != null && !genreIds.isEmpty()) {
//            query.setParameter("genreIds", genreIds);
//        }
//
//        if (tierCode != null && !tierCode.trim().isEmpty()) {
//            query.setParameter("tierCode", tierCode);
//        }
//
//        List<UserSheet> results = query.getResultList();
//        log.info("검색 결과: {}건", results.size());
//
//        return results;
//    }

    @Override
    public List<UserSheet> findCompletedSheets(Integer userId) {
        log.info("완주한 곡 조회 - userId: {}", userId);

        String jpql = "SELECT us FROM UserSheet us " +
                "JOIN FETCH us.sheet s " +
                "JOIN FETCH s.tier t " +
                "WHERE us.user.userId = :userId " +
                "AND us.progressRate = 100 " +
                "AND us.deletedYes = false " +
                "ORDER BY t.tierId DESC, us.bestScore DESC NULLS LAST, us.createdAt DESC";

        TypedQuery<UserSheet> query = entityManager.createQuery(jpql, UserSheet.class);
        query.setParameter("userId", userId);
        query.setMaxResults(50);

        List<UserSheet> results = query.getResultList();
        log.info("완주한 곡: {}건", results.size());

        return results;
    }
}