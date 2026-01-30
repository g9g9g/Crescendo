package com.d102.crescendo.domain.rank.service;

import com.d102.crescendo.domain.rank.dto.response.RankResponse;
import com.d102.crescendo.domain.rank.entity.RankId;
import com.d102.crescendo.domain.rank.entity.UserInstrumentRankDaily;
import com.d102.crescendo.domain.rank.repository.RankRepository;
import com.d102.crescendo.domain.sheet.entity.Instrument;
import com.d102.crescendo.domain.sheet.repository.InstrumentRepository;
import com.d102.crescendo.domain.user.entity.UserInstrumentTier;
import com.d102.crescendo.domain.user.repository.UserInstrumentTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankService {

    private final RankRepository rankRepository;
    private final UserInstrumentTierRepository userInstrumentTierRepository;
    private final InstrumentRepository instrumentRepository;

    public RankResponse getTop20Rankings(Integer instrumentId) {
        LocalDate today = LocalDate.now();  // yyyy-MM-dd

        List<UserInstrumentRankDaily> rankings = rankRepository
                .findTop20ByInstrumentAndDate(today, instrumentId);

        List<RankResponse.RankUserInfo> topUsers = rankings.stream()
                .limit(20)
                .map(rank -> {
                    UserInstrumentTier userTier = rank.getUser().getUserInstrumentTiers().stream()
                            .filter(uit -> uit.getId().getInstrumentId().equals(instrumentId))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("User instrument tier not found"));

                    return RankResponse.RankUserInfo.builder()
                            .rank(rank.getRank())
                            .nickname(rank.getUser().getNickname())
                            .profileUrl(rank.getUser().getProfileUrl())
                            .tierCode(userTier.getTier().getTierCode())
                            .tierLevel(userTier.getTier().getTierLevel())
                            .exp(userTier.getExp())
                            .build();
                })
                .collect(Collectors.toList());

        return RankResponse.builder()
                .topUsers(topUsers)
                .build();
    }

    /**
     * 모든 악기에 대해 일일 랭킹을 계산하고 저장
     * 매일 자정에 실행됨
     */
    @Transactional
    public void calculateAndSaveDailyRankings() {
        log.info("Starting daily ranking calculation...");
        LocalDate today = LocalDate.now();

        // 모든 악기 조회
        List<Instrument> instruments = instrumentRepository.findAll();

        List<UserInstrumentRankDaily> allRankings = new ArrayList<>();

        for (Instrument instrument : instruments) {
            log.info("Calculating rankings for instrument: {} (ID: {})",
                    instrument.getName(), instrument.getInstrumentId());

            // 해당 악기의 모든 사용자를 exp 순으로 정렬하여 조회
            List<UserInstrumentTier> userTiers = userInstrumentTierRepository
                    .findAllByInstrumentIdOrderByExpDesc(instrument.getInstrumentId());

            // 순위 부여 및 UserInstrumentRankDaily 생성
            int rank = 1;
            for (UserInstrumentTier userTier : userTiers) {
                RankId rankId = new RankId(
                        today,
                        userTier.getUser().getUserId(),
                        instrument.getInstrumentId()
                );

                UserInstrumentRankDaily ranking = new UserInstrumentRankDaily(
                        rankId,
                        userTier.getUser(),
                        instrument,
                        rank,
                        null  // createdAt은 @CreationTimestamp로 자동 설정
                );

                allRankings.add(ranking);
                rank++;
            }

            log.info("Calculated {} rankings for instrument: {}", userTiers.size(), instrument.getName());
        }

        // Bulk insert로 일괄 저장
        rankRepository.bulkInsertRankings(allRankings);
        log.info("Daily ranking calculation completed. Total rankings saved: {}", allRankings.size());
    }
}