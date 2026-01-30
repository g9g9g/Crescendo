package com.d102.crescendo.domain.rank.repository;

import com.d102.crescendo.domain.rank.entity.RankId;
import com.d102.crescendo.domain.rank.entity.UserInstrumentRankDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RankRepository extends JpaRepository<UserInstrumentRankDaily, RankId>, RankRepositoryCustom {

    @Query("SELECT r FROM UserInstrumentRankDaily r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH u.userInstrumentTiers uit " +
            "JOIN FETCH uit.tier t " +
            "WHERE r.id.rankDate = :rankDate " +
            "AND r.id.instrumentId = :instrumentId " +
            "AND uit.id.instrumentId = :instrumentId " +
            "ORDER BY r.rank ASC")
    List<UserInstrumentRankDaily> findTop20ByInstrumentAndDate(
            @Param("rankDate") LocalDate rankDate,
            @Param("instrumentId") Integer instrumentId
    );
}