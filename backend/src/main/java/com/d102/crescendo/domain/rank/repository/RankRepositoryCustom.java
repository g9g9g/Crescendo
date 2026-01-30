package com.d102.crescendo.domain.rank.repository;

import com.d102.crescendo.domain.rank.entity.UserInstrumentRankDaily;

import java.util.List;

public interface RankRepositoryCustom {

    /**
     * Bulk insert를 통한 대량 랭킹 데이터 저장
     * JDBC batch insert를 사용하여 성능 최적화
     *
     * @param rankings 저장할 랭킹 데이터 리스트
     */
    void bulkInsertRankings(List<UserInstrumentRankDaily> rankings);
}