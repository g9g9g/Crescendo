package com.d102.crescendo.domain.sheet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sheet_difficulty_metric")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SheetDifficultyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private SheetMusic sheet;

    @Column(nullable = false)
    private Double tempo;

    @Column(nullable = false)
    private Double rhythm;

    @Column(nullable = false)
    private Double intervals;

    @Column(nullable = false)
    private Double harmony;

    @Column(nullable = false)
    private Double technique;

    @Column(nullable = false)
    private Double length;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String recommendations;  // JSON 문자열로 저장 (List<String>)
}
