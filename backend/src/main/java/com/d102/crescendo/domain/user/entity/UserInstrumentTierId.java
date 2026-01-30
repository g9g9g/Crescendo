package com.d102.crescendo.domain.user.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserInstrumentTierId implements Serializable {
    private Integer userId;
    private Integer instrumentId;
}