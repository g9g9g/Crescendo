package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SheetMusicRepository extends JpaRepository<SheetMusic, Integer>, SheetMusicRepositoryCustom {

    /**
     * 다운로드 수 기준 상위 악보 조회 (SYSTEM 악보만)
     * @param pageable 페이지 정보 (top 10)
     * @return 인기 악보 리스트
     */
    @Query("SELECT s FROM SheetMusic s " +
            "WHERE s.visibleYes = true " +
            "AND s.sourceType = 'SYSTEM' " +
            "ORDER BY s.downloadNumber DESC")
    List<SheetMusic> findTopByDownloadNumber(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SheetMusic s set s.downloadNumber = s.downloadNumber + 1 where s.sheetId = :sheetId")
    int incrementDownload(Integer sheetId);


    /**
     * 서비스 악보 단건 조회 (SYSTEM 타입, visible=true, fetch join)
     */
    @Query("SELECT s FROM SheetMusic s " +
            "LEFT JOIN FETCH s.genre " +
            "LEFT JOIN FETCH s.tier " +
            "LEFT JOIN FETCH s.instrument " +
            "WHERE s.sheetId = :sheetId " +
            "AND s.sourceType = 'SYSTEM' " +
            "AND s.visibleYes = true")
    Optional<SheetMusic> findServiceSheetByIdWithDetails(@Param("sheetId") Integer sheetId);

    /**
     * 장르별 다운로드 수 기준 상위 악보 조회
     */
    @Query("SELECT s FROM SheetMusic s " +
            "LEFT JOIN FETCH s.genre " +
            "LEFT JOIN FETCH s.tier " +
            "LEFT JOIN FETCH s.instrument " +
            "WHERE s.sourceType = 'SYSTEM' " +
            "AND s.visibleYes = true " +
            "AND s.genre.genreId = :genreId " +
            "AND s.instrument.instrumentId = :instrumentId " +
            "ORDER BY s.downloadNumber DESC")
    List<SheetMusic> findTopSheetsByGenreAndInstrument(@Param("genreId") Integer genreId,
                                                         @Param("instrumentId") Integer instrumentId,
                                                         Pageable pageable);

    Optional<SheetMusic> findByXmlUrl(String xmlUrl);

    /**
     * pgvector를 이용한 유사 악보 검색 (코사인 유사도 기반)
     * @param querySheetId 기준 악보 ID
     * @param limit 반환할 유사 악보 개수
     * @return 유사도가 높은 순으로 정렬된 악보 ID 리스트
     */
    @Query(value = "WITH query_embedding AS ( " +
            "  SELECT embedding FROM sheet_music WHERE sheet_id = :querySheetId " +
            ") " +
            "SELECT s.sheet_id " +
            "FROM sheet_music s, query_embedding q " +
            "WHERE s.embedding IS NOT NULL " +
            "AND s.sheet_id != :querySheetId " +
            "AND s.visible_yes = true " +
            "AND s.source_type = 'SYSTEM' " +
            "ORDER BY s.embedding <=> q.embedding " +
            "LIMIT :limit", nativeQuery = true)
    List<Integer> findSimilarSheetsByEmbedding(@Param("querySheetId") Integer querySheetId, @Param("limit") Integer limit);

    /**
     * 제목 존재 여부 확인
     */
    boolean existsByTitle(String title);
}