package com.d102.crescendo.domain.user.repository;

import com.d102.crescendo.domain.user.entity.UserGenre;
import com.d102.crescendo.domain.user.entity.UserGenreId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGenreRepository extends JpaRepository<UserGenre, UserGenreId> {
}