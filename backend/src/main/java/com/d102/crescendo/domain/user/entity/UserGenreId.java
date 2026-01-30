package com.d102.crescendo.domain.user.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserGenreId implements Serializable {

    private Integer userId;
    private Integer genreId;
}