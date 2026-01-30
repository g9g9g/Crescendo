package com.d102.crescendo.domain.recommendation.service;

import com.d102.crescendo.domain.recommendation.dto.UserPlayRecord;
import com.d102.crescendo.domain.sheet.entity.*;
import com.d102.crescendo.domain.sheet.repository.GenreRepository;
import com.d102.crescendo.domain.user.entity.UserGenre;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 하이브리드 벡터 생성 및 코사인 유사도 계산 서비스
 *
 * 벡터 구성 (3-파트 독립 정규화 방식):
 * - Part 1: 난이도 지표 6개 (Min-Max 정규화 + L2 정규화)
 * - Part 2: 장르 One-Hot (N차원, L2 정규화)
 * - Part 3: 음표 임베딩 (1024차원, L2 정규화)
 *
 * 총 차원: 6 + N + 1024 (약 1040차원)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorGenerationService {

    private final GenreRepository genreRepository;
    private final NormalizationService normalizationService;

    // 가중치 상수
    private static final double WEIGHT_EXPLICIT_GENRE = 10.0;  // 명시적 선호 (사용 중)

    // [DEPRECATED] 아래 상수들은 개선된 가중치 계산으로 대체됨 (참고용 유지)
    // private static final double WEIGHT_COMPLETION = 3.0;       // 완주 → 진도율 연속 반영으로 대체
    // private static final double WEIGHT_RECENT = 2.0;           // 최근 7일 → 시간 감쇠로 대체
    // private static final double WEIGHT_PRACTICE_HIGH = 1.0;    // 연습시간 상위 20% → 정규화로 대체

    // 벡터 차원 상수
    private static final int DIFFICULTY_DIM = 6;      // 난이도 지표 차원
    private static final int EMBEDDING_DIM = 1024;    // 음표 임베딩 차원

    // 차원 정보 (애플리케이션 시작 시 초기화)
    private Map<Integer, Integer> genreIdToIndex;
    private int genreDim;  // 장르 차원 수

    @Getter
    private int totalDimensions;  // getter: 총 벡터 차원 반환

    /**
     * 애플리케이션 시작 시 차원 정보 초기화
     */
    @PostConstruct
    public void init() {
        List<Genre> genres = genreRepository.findAll();

        // 장르 ID → 인덱스 매핑
        genreIdToIndex = new HashMap<>();
        for (int i = 0; i < genres.size(); i++) {
            genreIdToIndex.put(genres.get(i).getGenreId(), i);
        }
        genreDim = genres.size();

        // 총 차원 계산: 난이도(6) + 장르(N) + 임베딩(1024)
        totalDimensions = DIFFICULTY_DIM + genreDim + EMBEDDING_DIM;

        log.info("하이브리드 벡터 차원 초기화 완료 - 총 {}차원 (난이도:{}, 장르:{}, 임베딩:{})",
                totalDimensions, DIFFICULTY_DIM, genreDim, EMBEDDING_DIM);
    }

    /**
     * 악보 아이템 벡터 생성 (하이브리드 3-파트)
     *
     * @param sheet 악보 엔티티
     * @return 하이브리드 벡터 (난이도 + 장르 + 임베딩, 각각 L2 정규화)
     */
    public double[] generateItemVector(SheetMusic sheet) {
        // Part 1: 난이도 벡터 (6차원)
        double[] difficultyVector = new double[DIFFICULTY_DIM];
        SheetDifficultyMetric metric = sheet.getDifficultyMetric();
        if (metric != null) {
            difficultyVector[0] = normalizationService.normalize(metric.getTempo(), "tempo");
            difficultyVector[1] = normalizationService.normalize(metric.getRhythm(), "rhythm");
            difficultyVector[2] = normalizationService.normalize(metric.getIntervals(), "intervals");
            difficultyVector[3] = normalizationService.normalize(metric.getHarmony(), "harmony");
            difficultyVector[4] = normalizationService.normalize(metric.getTechnique(), "technique");
            difficultyVector[5] = normalizationService.normalize(metric.getLength(), "length");
        }
        difficultyVector = normalizeL2(difficultyVector);

        // Part 2: 장르 벡터 (N차원, 원-핫)
        double[] genreVector = new double[genreDim];
        if (sheet.getGenre() != null) {
            Integer genreIndex = genreIdToIndex.get(sheet.getGenre().getGenreId());
            if (genreIndex != null) {
                genreVector[genreIndex] = 1.0;
            }
        }
        genreVector = normalizeL2(genreVector);

        // Part 3: 음표 임베딩 (1024차원)
        double[] embeddingVector = new double[EMBEDDING_DIM];
        float[] embedding = sheet.getEmbedding();
        if (embedding != null && embedding.length == EMBEDDING_DIM) {
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                embeddingVector[i] = embedding[i];
            }
        }
        embeddingVector = normalizeL2(embeddingVector);

        // 3개 파트 결합
        return concatenateVectors(difficultyVector, genreVector, embeddingVector);
    }


    /**
     * 통합 사용자 벡터 생성 (모든 가용 데이터 활용, 3-파트 독립 정규화)
     *
     * P_user = [난이도_평균, 장르_평균, 임베딩_평균] (각각 L2 정규화 후 결합)
     *
     * @param activities        사용자 연주 기록 (비어있을 수 있음)
     * @param explicitGenres    명시적 선호 장르 (비어있을 수 있음)
     * @param librarySheets     라이브러리 악보 (비어있을 수 있음)
     * @param allSheets         모든 악보 엔티티 맵 (연주 + 라이브러리)
     * @return 하이브리드 사용자 프로필 벡터
     */
    public double[] generateFlexibleUserVector(
            List<UserPlayRecord> activities,
            Set<UserGenre> explicitGenres,
            List<com.d102.crescendo.domain.sheet.entity.UserSheet> librarySheets,
            Map<Integer, SheetMusic> allSheets
    ) {
        // 3-파트 누적 벡터 초기화
        double[] difficultyAcc = new double[DIFFICULTY_DIM];
        double[] genreAcc = new double[genreDim];
        double[] embeddingAcc = new double[EMBEDDING_DIM];

        // 1. 명시적 선호 장르 반영 (W = 10.0)
        if (explicitGenres != null && !explicitGenres.isEmpty()) {
            for (UserGenre userGenre : explicitGenres) {
                Integer genreIndex = genreIdToIndex.get(userGenre.getGenre().getGenreId());
                if (genreIndex != null) {
                    genreAcc[genreIndex] += WEIGHT_EXPLICIT_GENRE;
                }
            }
        }

        // 2. 연주 기록 기반 가중 누적
        if (activities != null && !activities.isEmpty()) {
            // 연습시간 Min-Max 계산 (정규화용)
            int minPracticeTime = activities.stream()
                    .map(UserPlayRecord::practiceTime)
                    .filter(Objects::nonNull)
                    .min(Integer::compare)
                    .orElse(0);
            int maxPracticeTime = activities.stream()
                    .map(UserPlayRecord::practiceTime)
                    .filter(Objects::nonNull)
                    .max(Integer::compare)
                    .orElse(1);

            for (UserPlayRecord activity : activities) {
                SheetMusic sheet = allSheets.get(activity.sheetId());
                if (sheet == null || sheet.getEmbedding() == null) {
                    continue; // 임베딩 없으면 스킵
                }

                // 가중치 계산 (개선된 버전)
                double weight = 1.0;

                // 1) 시간 감쇠: 최근일수록 높은 가중치 (지수 함수 감쇠)
                if (activity.lastAccessedAt() != null) {
                    long daysSincePlay = ChronoUnit.DAYS.between(
                            activity.lastAccessedAt(),
                            LocalDateTime.now()
                    );
                    double timeDecay = Math.exp(-daysSincePlay / 7.0); // 7일 반감기
                    weight *= (1.0 + timeDecay * 2.0); // 오늘: 3.0배, 7일: 1.74배, 14일: 1.27배
                }

                // 2) 진도율 반영: 연속적으로 반영 (0% ~ 100%)
                if (activity.progressRate() != null) {
                    double progressWeight = 1.0 + (activity.progressRate() / 100.0 * 2.0);
                    weight *= progressWeight; // 0%: 1.0배, 50%: 2.0배, 100%: 3.0배
                }

                // 3) 연습시간 정규화: Min-Max 정규화로 연속 반영
                if (activity.practiceTime() != null && maxPracticeTime > minPracticeTime) {
                    double normalizedTime = (activity.practiceTime() - minPracticeTime)
                            / (double) (maxPracticeTime - minPracticeTime);
                    weight *= (1.0 + normalizedTime); // 최소: 1.0배, 최대: 2.0배
                }

                // 가중치 상한선 (너무 큰 값 방지)
                weight = Math.min(weight, 15.0);

                // 각 파트별 가중 누적
                accumulateDifficultyPart(difficultyAcc, sheet, weight);
                accumulateGenrePart(genreAcc, sheet, weight);
                accumulateEmbeddingPart(embeddingAcc, sheet, weight);
            }
        }

        // 3. 라이브러리 악보 기반 누적 (연주하지 않은 악보만)
        if (librarySheets != null && !librarySheets.isEmpty()) {
            Set<Integer> playedSheetIds = new HashSet<>();
            if (activities != null) {
                playedSheetIds = activities.stream()
                        .map(UserPlayRecord::sheetId)
                        .collect(java.util.stream.Collectors.toSet());
            }

            for (UserSheet userSheet : librarySheets) {
                Integer sheetId = userSheet.getSheet().getSheetId();
                if (playedSheetIds.contains(sheetId)) {
                    continue;
                }

                SheetMusic sheet = allSheets.get(sheetId);
                if (sheet == null || sheet.getEmbedding() == null) {
                    continue; // 임베딩 없으면 스킵
                }

                // 라이브러리 가중치 1.0
                accumulateDifficultyPart(difficultyAcc, sheet, 1.0);
                accumulateGenrePart(genreAcc, sheet, 1.0);
                accumulateEmbeddingPart(embeddingAcc, sheet, 1.0);
            }
        }

        // 4. 각 파트 L2 정규화
        difficultyAcc = normalizeL2(difficultyAcc);
        genreAcc = normalizeL2(genreAcc);
        embeddingAcc = normalizeL2(embeddingAcc);

        // 5. 3개 파트 결합
        return concatenateVectors(difficultyAcc, genreAcc, embeddingAcc);
    }

    /**
     * 코사인 유사도 계산
     *
     * similarity = (A · B) / (||A|| × ||B||)
     *
     * @param vectorA 벡터 A
     * @param vectorB 벡터 B
     * @return 코사인 유사도 (-1.0 ~ 1.0, 높을수록 유사)
     */
    public double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("벡터 차원이 일치하지 않습니다.");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        // 0으로 나누기 방지
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    /**
     * [DEPRECATED] 연습시간 상위 N% 임계값 계산
     * → 연습시간 Min-Max 정규화로 대체됨 (참고용 유지)
     */
    // private int calculateTopPracticeThreshold(List<UserPlayRecord> activities, int topPercentage) {
    //     if (activities.isEmpty()) {
    //         return 0;
    //     }
    //
    //     List<Integer> practiceTimes = activities.stream()
    //             .map(UserPlayRecord::practiceTime)
    //             .filter(Objects::nonNull)
    //             .sorted(Comparator.reverseOrder())
    //             .toList();
    //
    //     if (practiceTimes.isEmpty()) {
    //         return 0;
    //     }
    //
    //     int topIndex = (int) Math.ceil(practiceTimes.size() * topPercentage / 100.0) - 1;
    //     topIndex = Math.max(0, Math.min(topIndex, practiceTimes.size() - 1));
    //
    //     return practiceTimes.get(topIndex);
    // }

    /**
     * 난이도 파트 가중 누적
     */
    private void accumulateDifficultyPart(double[] acc, SheetMusic sheet, double weight) {
        SheetDifficultyMetric metric = sheet.getDifficultyMetric();
        if (metric != null) {
            acc[0] += weight * normalizationService.normalize(metric.getTempo(), "tempo");
            acc[1] += weight * normalizationService.normalize(metric.getRhythm(), "rhythm");
            acc[2] += weight * normalizationService.normalize(metric.getIntervals(), "intervals");
            acc[3] += weight * normalizationService.normalize(metric.getHarmony(), "harmony");
            acc[4] += weight * normalizationService.normalize(metric.getTechnique(), "technique");
            acc[5] += weight * normalizationService.normalize(metric.getLength(), "length");
        }
    }

    /**
     * 장르 파트 가중 누적
     */
    private void accumulateGenrePart(double[] acc, SheetMusic sheet, double weight) {
        if (sheet.getGenre() != null) {
            Integer genreIndex = genreIdToIndex.get(sheet.getGenre().getGenreId());
            if (genreIndex != null) {
                acc[genreIndex] += weight;
            }
        }
    }

    /**
     * 임베딩 파트 가중 누적
     */
    private void accumulateEmbeddingPart(double[] acc, SheetMusic sheet, double weight) {
        float[] embedding = sheet.getEmbedding();
        if (embedding != null && embedding.length == EMBEDDING_DIM) {
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                acc[i] += weight * embedding[i];
            }
        }
    }

    /**
     * L2 정규화 (벡터를 단위 벡터로 변환)
     *
     * normalized = vector / ||vector||
     * where ||vector|| = sqrt(Σ(element²))
     *
     * @param vector 원본 벡터
     * @return L2 정규화된 벡터 (단위 벡터)
     */
    private double[] normalizeL2(double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        // 제로 벡터 방지
        if (norm < 1e-10) {
            return vector;
        }

        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }

    /**
     * 여러 벡터를 하나로 연결 (concatenate)
     *
     * @param vectors 연결할 벡터들
     * @return 연결된 단일 벡터
     */
    private double[] concatenateVectors(double[]... vectors) {
        int totalLength = 0;
        for (double[] vec : vectors) {
            totalLength += vec.length;
        }

        double[] result = new double[totalLength];
        int offset = 0;
        for (double[] vec : vectors) {
            System.arraycopy(vec, 0, result, offset, vec.length);
            offset += vec.length;
        }
        return result;
    }

}
