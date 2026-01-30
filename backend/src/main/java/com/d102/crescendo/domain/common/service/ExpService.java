package com.d102.crescendo.domain.common.service;

import com.d102.crescendo.domain.common.entity.ExpLog;
import com.d102.crescendo.domain.common.entity.Tier;
import com.d102.crescendo.domain.common.repository.ExpLogRepository;
import com.d102.crescendo.domain.common.repository.TierRepository;
import com.d102.crescendo.domain.sheet.entity.Instrument;
import com.d102.crescendo.domain.sheet.entity.SheetMusic;
import com.d102.crescendo.domain.sheet.entity.UserSheet;
import com.d102.crescendo.domain.sheet.repository.UserSheetRepository;
import com.d102.crescendo.domain.user.entity.User;
import com.d102.crescendo.domain.user.entity.UserInstrumentTier;
import com.d102.crescendo.domain.user.repository.UserInstrumentTierRepository;
import com.d102.crescendo.global.exception.BusinessException;
import com.d102.crescendo.global.exception.BusinessError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * 경험치 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpService {

    private final ExpLogRepository expLogRepository;
    private final UserInstrumentTierRepository userInstrumentTierRepository;
    private final TierRepository tierRepository;
    private final UserSheetRepository userSheetRepository;

    /**
     * 악보 완료 후 경험치 지급
     * 조건:
     * 1. 진도율 100%
     * 2. source_type == SYSTEM (시스템 악보)
     * 3. 해당 악보로 이미 경험치를 받지 않았을 것
     *
     * @param userSheetId 완료한 사용자 악보 ID
     * @return 티어 레벨업 정보 (레벨업한 경우), null (레벨업하지 않은 경우 또는 조건 미충족)
     */
    @Transactional(propagation = REQUIRES_NEW)
    public TierUpgradeInfo grantExpForCompletion(Integer userSheetId) {
        // UserSheet 조회 (모든 연관 엔티티를 fetch join으로 로드)
        UserSheet userSheet = userSheetRepository.findUserSheetDetailById(userSheetId)
                .orElseThrow(() -> {
                    log.error("UserSheet를 찾을 수 없음 - userSheetId: {}", userSheetId);
                    return new BusinessException(BusinessError.USER_SHEET_NOT_FOUND);
                });

        User user = userSheet.getUser();
        SheetMusic sheet = userSheet.getSheet();
        Instrument instrument = sheet.getInstrument();
        Tier sheetTier = sheet.getTier();

        log.info("경험치 지급 시도 - userId: {}, sheetId: {}, progressRate: {}",
                user.getUserId(), sheet.getSheetId(), userSheet.getProgressRate());

        // 1. 진도율 100% 체크
        if (userSheet.getProgressRate() == null || userSheet.getProgressRate() < 100) {
            log.info("진도율 100% 미달 - userId: {}, sheetId: {}, progressRate: {}",
                    user.getUserId(), sheet.getSheetId(), userSheet.getProgressRate());
            return null;
        }

        // 2. 시스템 악보인지 체크
        if (sheet.getSourceType() != SheetMusic.SourceType.SYSTEM) {
            log.info("시스템 악보가 아님 - userId: {}, sheetId: {}, sourceType: {}",
                    user.getUserId(), sheet.getSheetId(), sheet.getSourceType());
            return null;
        }

        // 3. 티어 정보 확인
        if (sheetTier == null) {
            log.warn("악보에 티어 정보가 없음 - sheetId: {}", sheet.getSheetId());
            return null;
        }

        // 4. 이미 경험치를 받았는지 체크
        boolean alreadyRewarded = expLogRepository.existsByUserIdAndSheetId(
                user.getUserId(), sheet.getSheetId());

        if (alreadyRewarded) {
            log.info("이미 경험치를 받은 악보 - userId: {}, sheetId: {}",
                    user.getUserId(), sheet.getSheetId());
            return null;
        }

        // 5. UserInstrumentTier 조회
        UserInstrumentTier userInstrumentTier = userInstrumentTierRepository
                .findByUserIdAndInstrumentId(user.getUserId(), instrument.getInstrumentId())
                .orElseThrow(() -> {
                    log.error("UserInstrumentTier를 찾을 수 없음 - userId: {}, instrumentId: {}",
                            user.getUserId(), instrument.getInstrumentId());
                    return new BusinessException(BusinessError.USER_INSTRUMENT_TIER_NOT_FOUND);
                });

        Tier currentTier = userInstrumentTier.getTier();
        Integer currentExp = userInstrumentTier.getExp();
        Short expReward = sheetTier.getExpReward();

        // 6. 경험치 증가
        userInstrumentTierRepository.addExp(user.getUserId(), instrument.getInstrumentId(), (int) expReward);
        Integer newExp = currentExp + expReward;

        // 7. 티어 레벨업 체크
        Tier newTier = currentTier;
        boolean tierUpgraded = false;

        // expToNext가 null이면 최고 티어 (레벨업 불가)
        if (currentTier.getExpToNext() != null && newExp >= currentTier.getExpToNext()) {
            // 다음 티어 조회
            Integer nextTierId = currentTier.getTierId() + 1;
            newTier = tierRepository.findByTierId(nextTierId)
                    .orElseThrow(() -> {
                        log.error("다음 티어를 찾을 수 없음 - tierId: {}", nextTierId);
                        return new IllegalStateException("다음 티어 정보를 찾을 수 없습니다.");
                    });

            // 다음 티어로 승급
            userInstrumentTierRepository.updateTier(
                    user.getUserId(),
                    instrument.getInstrumentId(),
                    nextTierId
            );
            tierUpgraded = true;

            log.info("티어 레벨업! - userId: {}, {} -> {}",
                    user.getUserId(), currentTier.getTierCode(), newTier.getTierCode());
        }

        // 8. ExpLog 기록
        ExpLog expLog = ExpLog.builder()
                .user(user)
                .instrument(instrument)
                .sheet(sheet)
                .tierBefore(currentTier)
                .tierAfter(newTier)
                .expBefore(currentExp)
                .expAfter(newExp)
                .expGained(expReward.byteValue())
                .build();

        expLogRepository.save(expLog);

        log.info("경험치 지급 완료 - userId: {}, sheetId: {}, expGained: {}, newExp: {}, tierUpgraded: {}",
                user.getUserId(), sheet.getSheetId(), expReward, newExp, tierUpgraded);

        // 티어 레벨업 정보 반환
        if (tierUpgraded) {
            String instrumentName = instrument.getInstrumentId() == 1 ? "피아노" : "기타";
            return new TierUpgradeInfo(
                    user.getUserId(),
                    instrumentName,
                    currentTier.getTierCode(),
                    newTier.getTierCode(),
                    currentTier.getTierLevel(),
                    newTier.getTierLevel()
            );
        }
        return null;
    }

    /**
     * 티어 레벨업 정보를 담는 DTO
     */
    public record TierUpgradeInfo(
            Integer userId,
            String instrumentName,
            String fromTierCode,
            String toTierCode,
            Short fromTierLevel,
            Short toTierLevel
    ) {}
}