package com.d102.crescendo.domain.user.entity;

import com.d102.crescendo.domain.sheet.entity.Genre;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGenre {

    @EmbeddedId
    private UserGenreId id;

    @MapsId("userId")  // 복합키의 userId 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("genreId")  // 복합키의 genreId 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;
}