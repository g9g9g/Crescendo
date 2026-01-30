package com.d102.crescendo.domain.fcm.dto.request;

import com.d102.crescendo.domain.fcm.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FcmSendRequest {
    private NotificationType type;
    private Integer userId;
    private String title;
    private String body;
}