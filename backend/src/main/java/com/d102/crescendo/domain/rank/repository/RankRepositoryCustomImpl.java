package com.d102.crescendo.domain.rank.repository;

import com.d102.crescendo.domain.rank.entity.UserInstrumentRankDaily;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RankRepositoryCustomImpl implements RankRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;

    @Override
    public void bulkInsertRankings(List<UserInstrumentRankDaily> rankings) {
        if (rankings.isEmpty()) {
            log.info("No rankings to insert");
            return;
        }

        String sql = "INSERT INTO user_instrument_rank_daily " +
                "(rank_date, user_id, instrument_id, rank, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now();
        int totalSize = rankings.size();

        // Batch 단위로 나누어 처리
        for (int i = 0; i < totalSize; i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, totalSize);
            List<UserInstrumentRankDaily> batch = rankings.subList(i, endIndex);

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int index) throws SQLException {
                    UserInstrumentRankDaily ranking = batch.get(index);
                    ps.setObject(1, ranking.getId().getRankDate());
                    ps.setInt(2, ranking.getId().getUserId());
                    ps.setInt(3, ranking.getId().getInstrumentId());
                    ps.setInt(4, ranking.getRank());
                    ps.setTimestamp(5, Timestamp.valueOf(now));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            log.info("Batch insert completed: {}/{} rankings", endIndex, totalSize);
        }

        log.info("Bulk insert completed: {} rankings inserted", totalSize);
    }
}