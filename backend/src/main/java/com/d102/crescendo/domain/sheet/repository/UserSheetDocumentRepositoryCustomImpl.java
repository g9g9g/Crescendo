package com.d102.crescendo.domain.sheet.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.d102.crescendo.domain.sheet.document.UserSheetDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Repository
@RequiredArgsConstructor
public class UserSheetDocumentRepositoryCustomImpl implements UserSheetDocumentRepositoryCustom {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public SearchHits<UserSheetDocument> searchUserSheets(
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
    ) {
        log.info("Elasticsearch 내 악보 검색 - userId: {}, keyword: {}, instrumentId: {}, genreIds: {}, tierCode: {}, tierLevel: {}-{}, sortType: {}, page: {}, size: {}",
                userId, searchKeyword, instrumentId, genreIds, tierCode, minTierLevel, maxTierLevel, sortType, page, size);

        // 1. 필수 조건 (userId, deletedYes)
        List<Query> mustConditions = new ArrayList<>();
        mustConditions.add(TermQuery.of(t -> t.field("userId").value(userId))._toQuery());
        mustConditions.add(TermQuery.of(t -> t.field("deletedYes").value(false))._toQuery());

        // 2. 검색어 조건 (제목, 작곡가, 악기, 장르 - 가중치 적용 및 오타 허용 + prefix 매칭)
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            List<Query> shouldConditions = new ArrayList<>();

            // 제목 검색 - 완전 매칭 (가중치 3.0, fuzziness AUTO)
            shouldConditions.add(MatchQuery.of(m -> m
                    .field("title")
                    .query(searchKeyword)
                    .boost(3.0f)
                    .fuzziness("AUTO"))._toQuery());

            // 제목 검색 - prefix 매칭 (가중치 2.5)
            shouldConditions.add(MatchPhrasePrefixQuery.of(m -> m
                    .field("title")
                    .query(searchKeyword)
                    .boost(2.5f))._toQuery());

            // 작곡가 검색 - 완전 매칭 (가중치 2.0, fuzziness AUTO)
            shouldConditions.add(MatchQuery.of(m -> m
                    .field("composer")
                    .query(searchKeyword)
                    .boost(2.0f)
                    .fuzziness("AUTO"))._toQuery());

            // 작곡가 검색 - prefix 매칭 (가중치 1.5)
            shouldConditions.add(MatchPhrasePrefixQuery.of(m -> m
                    .field("composer")
                    .query(searchKeyword)
                    .boost(1.5f))._toQuery());

            // 장르 검색 (가중치 1.0)
            shouldConditions.add(MatchQuery.of(m -> m
                    .field("genre")
                    .query(searchKeyword)
                    .boost(1.0f))._toQuery());

            // 악기 검색 (가중치 1.0)
            shouldConditions.add(MatchQuery.of(m -> m
                    .field("instrument")
                    .query(searchKeyword)
                    .boost(1.0f))._toQuery());

            mustConditions.add(BoolQuery.of(b -> b
                    .should(shouldConditions)
                    .minimumShouldMatch("1"))
                    ._toQuery());
        }

        // 3. 악기 필터
        if (instrumentId != null) {
            mustConditions.add(TermQuery.of(t -> t.field("instrumentId").value(instrumentId))._toQuery());
        }

        // 4. 장르 필터 (복수 선택 가능)
        if (genreIds != null && !genreIds.isEmpty()) {
            List<Query> genreQueries = new ArrayList<>();
            for (Integer genreId : genreIds) {
                genreQueries.add(TermQuery.of(t -> t.field("genreId").value(genreId))._toQuery());
            }
            mustConditions.add(BoolQuery.of(b -> b
                    .should(genreQueries)
                    .minimumShouldMatch("1"))
                    ._toQuery());
        }

        // 5. 티어 코드 필터
        if (tierCode != null && !tierCode.trim().isEmpty()) {
            mustConditions.add(TermQuery.of(t -> t.field("tierCode").value(tierCode))._toQuery());
        }

        // 6. 티어 레벨 범위 필터
        if (minTierLevel != null && maxTierLevel != null) {
            mustConditions.add(NumberRangeQuery.of(r -> r
                    .field("tierLevel")
                    .gte((double) minTierLevel)
                    .lte((double) maxTierLevel)
            )._toRangeQuery()._toQuery());
        } else if (minTierLevel != null) {
            mustConditions.add(NumberRangeQuery.of(r -> r
                    .field("tierLevel")
                    .gte((double) minTierLevel)
            )._toRangeQuery()._toQuery());
        } else if (maxTierLevel != null) {
            mustConditions.add(NumberRangeQuery.of(r -> r
                    .field("tierLevel")
                    .lte((double) maxTierLevel)
            )._toRangeQuery()._toQuery());
        }

        // Bool Query 생성
        Query boolQuery = BoolQuery.of(b -> b.must(mustConditions))._toQuery();

        // 7. 정렬 조건 설정
        Sort sort;
        if (sortType == null || sortType == 1) {
            // 최신순 (lastAccessedAt 내림차순)
            sort = Sort.by(Sort.Direction.DESC, "lastAccessedAt");
        } else if (sortType == 2) {
            // 진행률순 (progressRate 내림차순, lastAccessedAt 내림차순)
            sort = Sort.by(Sort.Direction.DESC, "progressRate")
                    .and(Sort.by(Sort.Direction.DESC, "lastAccessedAt"));
        } else if (sortType == 3) {
            // 제목순 (title.keyword 오름차순)
            sort = Sort.by(Sort.Direction.ASC, "title.keyword");
        } else {
            // 기본값: 최신순
            sort = Sort.by(Sort.Direction.DESC, "lastAccessedAt");
        }

        // 8. 페이지네이션 설정
        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 5;
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        // NativeQuery 생성
        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery)
                .withPageable(pageable)
                .build();

        // 검색 수행
        SearchHits<UserSheetDocument> searchHits = elasticsearchOperations.search(query, UserSheetDocument.class);

        log.info("Elasticsearch 검색 결과: {}건 (전체: {}건)", searchHits.getSearchHits().size(), searchHits.getTotalHits());

        return searchHits;
    }

    @Override
    public SearchHits<UserSheetDocument> autocompleteUserSheets(
            Integer userId,
            String keyword,
            Integer size
    ) {
        log.info("Elasticsearch 내 악보 자동완성 - userId: {}, keyword: {}, size: {}", userId, keyword, size);

        // 1. 필수 조건 (userId, deletedYes)
        List<Query> mustConditions = new ArrayList<>();
        mustConditions.add(TermQuery.of(t -> t.field("userId").value(userId))._toQuery());
        mustConditions.add(TermQuery.of(t -> t.field("deletedYes").value(false))._toQuery());

        // 2. 자동완성 검색 조건 (title 또는 composer prefix 검색)
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<Query> shouldConditions = new ArrayList<>();

            // title로 prefix 검색 (match_phrase_prefix 사용)
            shouldConditions.add(MatchPhrasePrefixQuery.of(m -> m
                    .field("title")
                    .query(keyword)
                    .boost(2.0f))._toQuery());

            // composer로 prefix 검색 (match_phrase_prefix 사용)
            shouldConditions.add(MatchPhrasePrefixQuery.of(m -> m
                    .field("composer")
                    .query(keyword)
                    .boost(1.0f))._toQuery());

            mustConditions.add(BoolQuery.of(b -> b
                    .should(shouldConditions)
                    .minimumShouldMatch("1"))
                    ._toQuery());
        }

        // Bool Query 생성
        Query boolQuery = BoolQuery.of(b -> b.must(mustConditions))._toQuery();

        // 3. 정렬: 점수순 (relevance)
        Sort sort = Sort.by(Sort.Direction.DESC, "_score");

        // 4. 페이지네이션 (최대 size개)
        int pageSize = (size != null && size > 0) ? size : 5;
        Pageable pageable = PageRequest.of(0, pageSize, sort);

        // NativeQuery 생성
        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery)
                .withPageable(pageable)
                .build();

        // 검색 수행
        SearchHits<UserSheetDocument> searchHits = elasticsearchOperations.search(query, UserSheetDocument.class);

        log.info("Elasticsearch 자동완성 결과: {}건", searchHits.getSearchHits().size());

        return searchHits;
    }
}