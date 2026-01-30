package com.d102.crescendo.domain.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Tier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tierId;

    @Column(nullable = false, length = 32)
    private String tierCode;

    @Column(nullable = false)
    private Short tierLevel;

    private Integer expToNext;  // 다음 레벨까지 필요한 총 경험치

    @Column(nullable = false)
    private Short expReward;
}
