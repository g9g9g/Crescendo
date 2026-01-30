package com.d102.crescendo.domain.recommendation.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;



@Entity
@Table(
        name = "daily_recommendation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_rec",
                        columnNames = {"user_id", "rec_date", "rank"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private  Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "rec_date", nullable = false)
    private LocalDate recDate;


    // 굳이 연관관계 안 걸고, id만 들고 가는 방식 (MSA/분리 생각)
    @Column(name = "sheet_id", nullable = false)
    private Integer sheetId;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "score", nullable = false)
    private double score;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
