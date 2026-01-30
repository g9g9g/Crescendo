package com.d102.crescendo.domain.user.repository;

import com.d102.crescendo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndDeletedYes(String email, boolean deletedYes);
    boolean existsByNickname(String nickname);

    /**
     * 모든 유저 ID 조회 (삭제되지 않은 유저)
     */
    @Query("SELECT u.userId FROM User u WHERE u.deletedYes = false")
    List<Integer> findAllUserIds();

    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.userGenres ug " +
           "LEFT JOIN FETCH ug.genre " +
           "LEFT JOIN FETCH u.userInstrumentTiers uit " +
           "LEFT JOIN FETCH uit.instrument " +
           "LEFT JOIN FETCH uit.tier " +
           "LEFT JOIN FETCH u.userInstrumentRankDailies uird " +
           "LEFT JOIN FETCH uird.instrument " +
           "WHERE u.userId = :userId")
    Optional<User> findByIdWithDetails(@Param("userId") Integer userId);

    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.userSheets " +
           "WHERE u.userId = :userId")
    Optional<User> findByIdWithUserSheets(@Param("userId") Integer userId);
}