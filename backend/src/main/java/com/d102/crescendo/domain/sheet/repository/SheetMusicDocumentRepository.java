package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.document.SheetMusicDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SheetMusicDocumentRepository extends ElasticsearchRepository<SheetMusicDocument, String> {
}