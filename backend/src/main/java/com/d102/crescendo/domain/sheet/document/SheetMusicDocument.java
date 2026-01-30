package com.d102.crescendo.domain.sheet.document;

import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "sheet_music")
@Setting(settingPath = "/elasticsearch/es-settings.json")
public class SheetMusicDocument {

    @Id
    @Field(type = FieldType.Integer)
    private Integer sheetId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "sheets_title_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword),
                    @InnerField(suffix = "auto_complete", type = FieldType.Search_As_You_Type)
            }
    )
    private String title;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "sheets_composer_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword),
                    @InnerField(suffix = "auto_complete", type = FieldType.Search_As_You_Type)
            }
    )
    private String composer;

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    @Field(type = FieldType.Integer)
    private Integer genreId;

    @Field(type = FieldType.Keyword)
    private String tierCode;

    @Field(type = FieldType.Integer)
    private Integer tierLevel;

    @Field(type = FieldType.Integer)
    private Integer instrumentId;

    @Field(type = FieldType.Integer)
    private Integer downloadNumber;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Keyword)
    private SheetMusic.SourceType sourceType;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "sheets_category_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            })
    private String genre;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "sheets_category_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            })
    private String instrument;
}
