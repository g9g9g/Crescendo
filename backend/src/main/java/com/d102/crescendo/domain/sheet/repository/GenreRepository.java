package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Integer> {
}