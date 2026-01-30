package com.d102.crescendo.domain.user.repository;

import com.d102.crescendo.domain.user.entity.UserInstrumentTier;
import com.d102.crescendo.domain.user.entity.UserInstrumentTierId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserInstrumentTierRepository extends JpaRepository<UserInstrumentTier, UserInstrumentTierId> {

    @Query("SELECT uit FROM UserInstrumentTier uit " +
            "JOIN FETCH uit.user u " +
            "JOIN FETCH uit.tier t " +
            "WHERE uit.id.instrumentId = :instrumentId " +
            "ORDER BY uit.exp DESC, u.userId ASC")
    List<UserInstrumentTier> findAllByInstrumentIdOrderByExpDesc(@Param("instrumentId") Integer instrumentId);

    @Query("SELECT uit FROM UserInstrumentTier uit " +
            "JOIN FETCH uit.tier t " +
            "WHERE uit.id.userId = :userId AND uit.id.instrumentId = :instrumentId")
    Optional<UserInstrumentTier> findByUserIdAndInstrumentId(@Param("userId") Integer userId, @Param("instrumentId") Integer instrumentId);

    @Modifying
    @Query("UPDATE UserInstrumentTier uit " +
            "SET uit.exp = uit.exp + :expGained " +
            "WHERE uit.id.userId = :userId AND uit.id.instrumentId = :instrumentId")
    void addExp(@Param("userId") Integer userId, @Param("instrumentId") Integer instrumentId, @Param("expGained") Integer expGained);

    @Modifying
    @Query("UPDATE UserInstrumentTier uit " +
            "SET uit.tier.tierId = :tierId " +
            "WHERE uit.id.userId = :userId AND uit.id.instrumentId = :instrumentId")
    void updateTier(@Param("userId") Integer userId, @Param("instrumentId") Integer instrumentId, @Param("tierId") Integer tierId);
}