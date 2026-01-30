package com.d102.crescendo.domain.rank.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RankId implements Serializable {

    private LocalDate rankDate;
    private Integer userId;
    private Integer instrumentId;
}
