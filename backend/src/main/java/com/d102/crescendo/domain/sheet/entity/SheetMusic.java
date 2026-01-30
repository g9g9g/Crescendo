package com.d102.crescendo.domain.sheet.entity;

import com.d102.crescendo.domain.common.entity.Tier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SheetMusic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sheetId;

    @Column(nullable = false, length = 100)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id")
    private Tier tier;

    @Column(length = 50)
    private String composer;

    @Column(length = 2048)
    private String thumbnailUrl;

    @Column(nullable = false, length = 2048)
    private String xmlUrl;

    @Column(length = 2048)
    private String xmlUrlPreview;

    @Builder.Default
    private Integer downloadNumber=0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private Short maxMeasureCnt = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_sheet_id")
    private SheetMusic originSheet;

    @Column(length = 500)
    private String style;

    @Column(nullable = false)
    private Boolean visibleYes = true;

    @JdbcTypeCode(SqlTypes.OTHER)
    @Convert(converter = VectorConverter.class)
    @Column(columnDefinition = "vector(1024)")
    private float[] embedding;

    @OneToOne(mappedBy = "sheet", fetch = FetchType.LAZY)
    private SheetDifficultyMetric difficultyMetric;

    public enum SourceType {
        SYSTEM, USER, ARRANGED
    }

    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public void updateTier(Tier tier) {
        this.tier = tier;
    }

    public void hideSheet() {
        this.visibleYes = false;
    }

    public void showSheet() {
        this.visibleYes = true;
    }

    // Admin update methods
    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateComposer(String composer) {
        this.composer = composer;
    }

    public void updateGenre(Genre genre) {
        this.genre = genre;
    }

    public void updateInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public void updateThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updateXmlUrl(String xmlUrl) {
        this.xmlUrl = xmlUrl;
    }

    public void updateXmlUrlPreview(String xmlUrlPreview) {
        this.xmlUrlPreview = xmlUrlPreview;
    }

    public void updateMaxMeasureCnt(Short maxMeasureCnt) {
        this.maxMeasureCnt = maxMeasureCnt;
    }

    public void updateSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public void updateOriginSheet(SheetMusic originSheet) {
        this.originSheet = originSheet;
    }

    public void updateStyle(String style) {
        this.style = style;
    }

    public void updateDownloadNumber(Integer downloadNumber) {
        this.downloadNumber = downloadNumber;
    }

    public void updateVisibleYes(Boolean visibleYes) {
        this.visibleYes = visibleYes;
    }

    public void updateCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void updateUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
