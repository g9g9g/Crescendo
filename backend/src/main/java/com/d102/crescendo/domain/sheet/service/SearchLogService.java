package com.d102.crescendo.domain.sheet.service;

import com.d102.crescendo.domain.sheet.document.SearchLogDocument;
import com.d102.crescendo.domain.sheet.repository.SearchLogRepository;
import com.d102.crescendo.domain.sheet.util.SearchQueryNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogService {

    private final SearchLogRepository searchLogRepository;

    public void saveSearchLog(String query, Integer userId) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        try {
            // 검색어 정규화: 공백 제거, 소문자 변환, 특수문자 제거
            String normalizedQuery = SearchQueryNormalizer.normalize(query);

            if (normalizedQuery == null || normalizedQuery.isEmpty()) {
                log.warn("Normalized query is empty after normalization: original={}", query);
                return;
            }

            SearchLogDocument logDocument = SearchLogDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .query(normalizedQuery)
                    .userId(userId)
                    .timestamp(Instant.now())
                    .build();

            searchLogRepository.save(logDocument);
            log.debug("Saved search log: original={}, normalized={}, userId={}",
                      query, normalizedQuery, userId);
        } catch (Exception e) {
            log.error("Failed to save search log: query={}, userId={}", query, userId, e);
        }
    }
}
