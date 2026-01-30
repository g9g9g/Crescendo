package com.d102.crescendo.util

import androidx.compose.runtime.Composable
import com.d102.crescendo.R

/**
 * 헬퍼: 티어 코드와 레벨로 R.drawable ID를 반환
 * (HomeScreen, MySheetsScreen 공용)
 */
@Composable
fun getTierDrawable(tierCode: String?, tierLevel: Int?): Int {
    val tierCodeAndLevel = tierCode?.uppercase() + " " + tierLevel.toString()
    return when (tierCodeAndLevel) {
        "GOLD 1" -> R.drawable.ic_gold1_sheet
        "GOLD 2" -> R.drawable.ic_gold2_sheet
        "GOLD 3" -> R.drawable.ic_gold3_sheet
        "SILVER 1" -> R.drawable.ic_silver1_sheet
        "SILVER 2" -> R.drawable.ic_silver2_sheet
        "SILVER 3" -> R.drawable.ic_silver3_sheet
        "BRONZE 1" -> R.drawable.ic_bronze1_sheet
        "BRONZE 2" -> R.drawable.ic_bronze2_sheet
        "BRONZE 3" -> R.drawable.ic_bronze3_sheet
        else -> R.drawable.ic_bronze3_sheet // 기본값
    }
}

/**
 * 헬퍼: 장르 ID -> 한글 이름 (임시)
 * (HomeScreen, MySheetsScreen 공용)
 * TODO: 추후 CommonRepository (캐시)에서 가져오도록 수정
 */
fun getGenreName(genreId: Int): String {
    return when (genreId) {
        1 -> "클래식"
        2 -> "가요"
        3 -> "뉴에이지"
        4 -> "OST"
        5 -> "동요"
        else -> "CCM"
    }
}

/**
 * 헬퍼: 장르 한글 이름 -> 장르 ID
 * (악보 등록 시 사용)
 */
fun getGenreId(genreName: String): Int {
    return when (genreName) {
        "클래식" -> 1
        "가요" -> 2
        "뉴에이지" -> 3
        "OST" -> 4
        "동요" -> 5
        "CCM" -> 6
        else -> 1 // 기본값
    }
}

/**
 * 헬퍼: 악기 한글 이름 -> 악기 ID
 * (악보 등록 시 사용)
 */
fun getInstrumentId(instrumentName: String): Int {
    return when (instrumentName) {
        "피아노" -> 1
        "기타" -> 2
        else -> 1 // 기본값 피아노
    }
}

/**
 * 헬퍼: 악기 ID -> R.drawable ID
 * (HomeScreen, MySheetsScreen 공용)
 */
fun getInstrumentDrawable(instrumentId: Int): Int {
    return when (instrumentId) {
        1 -> R.drawable.ic_piano
        2 -> R.drawable.ic_guitar
        else -> R.drawable.ic_close // TODO: 기본 아이콘 확인
    }
}