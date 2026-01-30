package com.d102.crescendo.domain.performance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "performance_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id", nullable = false)
    private Integer evaluationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "play_id", nullable = false)
    private Performance performance;

    private Short tempoStability; // 박자 안정성

    private Short rhythmConsistency; // 리듬 일관성

    private Short dynamicsExpression; // 다이내믹 표현력

    private Short articulationBalance; // 아티큘레이션 균형

    private Short cleanTechnique; // 깔끔한 연주 기술

    private Short pitchDiversity; // 음역대 다양성

    private Short polyphonyControl; // 화음 제어

    private Short phraseVariety; // 프레이즈 다양성

    private Short pacingBalance; // 페이싱 균형

    @Column(name = "comment", length = 400)
    private String comment; // AI 코멘트

    private Short score; // 총점

    private String grade;  // 등급
}
