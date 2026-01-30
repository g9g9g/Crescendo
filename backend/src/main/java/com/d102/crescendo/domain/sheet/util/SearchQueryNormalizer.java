package com.d102.crescendo.domain.sheet.util;

/**
 * 검색어 정규화 유틸리티
 *
 * 검색어를 정규화하여 동일한 의도의 검색어를 통합 집계할 수 있도록 합니다.
 *
 * 정규화 규칙:
 * 1. 앞뒤 공백 제거 (trim)
 * 2. 특수문자 제거 (영문, 숫자, 한글, 공백만 유지)
 * 3. 연속된 공백을 하나로 통일
 * 4. 가장 첫 글자가 영문인 경우 대문자로 변환
 *
 * 예시:
 * - "IU" → "IU"
 * - "iu" → "IU"
 * - "Begin-Again!" → "Begin Again"
 * - " my song " → "My song"
 */
public class SearchQueryNormalizer {

    /**
     * 검색어를 정규화합니다.
     *
     * @param query 원본 검색어
     * @return 정규화된 검색어 (null 또는 빈 문자열인 경우 null 반환)
     */
    public static String normalize(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        String normalized = query
                .trim()                          // 앞뒤 공백 제거
                .replaceAll("[^a-zA-Z0-9가-힣 ]", " ") // 특수문자를 공백으로 변환 (영문, 숫자, 한글, 공백만 유지)
                .replaceAll(" +", " ");          // 연속된 공백을 하나로 통일

        // 첫 글자가 영어인 경우 대문자로 변환
        if (!normalized.isEmpty() && Character.isLetter(normalized.charAt(0))) {
            char firstChar = normalized.charAt(0);
            if ((firstChar >= 'a' && firstChar <= 'z') || (firstChar >= 'A' && firstChar <= 'Z')) {
                normalized = Character.toUpperCase(firstChar) + normalized.substring(1);
            }
        }

        return normalized;
    }
}
