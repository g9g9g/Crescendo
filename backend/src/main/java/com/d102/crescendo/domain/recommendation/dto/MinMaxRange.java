package com.d102.crescendo.domain.recommendation.dto;

/**
 * Min-Max 정규화를 위한 최소/최대값 범위
 */
public record MinMaxRange(
        Double min,
        Double max
) {
    /**
     * 값을 0.0 ~ 1.0 사이로 Min-Max 정규화
     *
     * @param value 정규화할 원본 값
     * @return 정규화된 값 (0.0 ~ 1.0)
     */
    public double normalize(Double value) {
        if (value == null || min == null || max == null) {
            return 0.0;
        }

        // min과 max가 같으면 0.5 반환 (division by zero 방지)
        if (Math.abs(max - min) < 0.0001) {
            return 0.5;
        }

        // Min-Max 정규화: (value - min) / (max - min)
        double normalized = (value - min) / (max - min);

        // 0.0 ~ 1.0 범위로 클램핑
        return Math.max(0.0, Math.min(1.0, normalized));
    }
}