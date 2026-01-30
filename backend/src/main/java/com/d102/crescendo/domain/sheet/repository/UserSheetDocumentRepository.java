package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.document.UserSheetDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSheetDocumentRepository extends ElasticsearchRepository<UserSheetDocument, String>, UserSheetDocumentRepositoryCustom {
    /**
     * userId와 sheetId로 문서 조회
     */
    List<UserSheetDocument> findByUserIdAndSheetId(Integer userId, Integer sheetId);
}