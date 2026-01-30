package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.document.SearchLogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchLogRepository extends ElasticsearchRepository<SearchLogDocument, String> {
}
