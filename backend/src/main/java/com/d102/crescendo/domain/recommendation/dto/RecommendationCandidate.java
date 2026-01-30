package com.d102.crescendo.domain.recommendation.dto;

/**
 * 추천 후보 악보와 유사도 점수
 */
public record RecommendationCandidate(
        Integer sheetId,
        Double score
) implements Comparable<RecommendationCandidate> {

    /**
     * 점수 기준 내림차순 정렬 (높은 점수가 우선)
     */
    @Override
    public int compareTo(RecommendationCandidate other) {
        return Double.compare(other.score, this.score);
    }
}