package com.d102.crescendo.domain.sheet.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "search_logs")
public class SearchLogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String query;

    @Field(type = FieldType.Integer)
    private Integer userId;

    @Field(type = FieldType.Date)
    private Instant timestamp;
}
