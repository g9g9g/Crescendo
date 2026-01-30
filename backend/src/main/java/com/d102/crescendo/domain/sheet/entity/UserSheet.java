package com.d102.crescendo.domain.sheet.entity;

import com.d102.crescendo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userSheetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private SheetMusic sheet;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastAccessedAt;

    @Builder.Default
    private Integer practiceTime = 0;

    private Short endMeasure;

    @Builder.Default
    private Short progressRate = 0;

    private Short bestScore;

    @Column(nullable = false)
    private boolean deletedYes;

    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedYes = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public void updatePracticeTime(Integer additionalTime) {
        this.practiceTime += additionalTime;
    }

    public void updateProgress(Short endMeasure, Short progressRate) {
        this.endMeasure = endMeasure;
        this.progressRate = progressRate;
    }

    public void updateBestScore(Short newScore) {
        if (this.bestScore == null || newScore > this.bestScore) {
            this.bestScore = newScore;
        }
    }
}
