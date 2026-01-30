package com.d102.crescendo.domain.common.entity;

import com.d102.crescendo.domain.sheet.entity.Instrument;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "exp_log")
public class ExpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exp_log_id")
    private Integer expLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private SheetMusic sheet;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_before_id", nullable = false)
    private Tier tierBefore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_after_id", nullable = false)
    private Tier tierAfter;

    @Column(nullable = false)
    private Integer expBefore;

    @Column(nullable = false)
    private Integer expAfter;

    @Column(nullable = false)
    private Byte expGained;
}