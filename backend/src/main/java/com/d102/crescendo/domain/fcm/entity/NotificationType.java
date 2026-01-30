package com.d102.crescendo.domain.fcm.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    EVALUATION("연주 평가"),
    TIERUP("티어업");

    private final String description;
}