package com.d102.crescendo.domain.performance.entity;

import com.d102.crescendo.domain.sheet.entity.UserSheet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer playId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_sheet_id")
    private UserSheet userSheet;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime endedAt;

    @Column(nullable = false)
    private Short startMeasure;

    @Column(nullable = false)
    private Short endMeasure;

    @Column(nullable = false)
    private boolean practiceMode;

    @OneToOne(mappedBy = "performance", fetch = FetchType.LAZY)
    private PerformanceEvaluation evaluation;

    @Column(length = 2048)
    private String wavXmlUrl;
}