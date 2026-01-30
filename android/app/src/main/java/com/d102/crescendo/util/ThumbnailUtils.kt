package com.d102.crescendo.util

import com.d102.crescendo.R

private val thumbnails = listOf(
    // Classic (16개)
    R.drawable.lp_classic_1,
    R.drawable.lp_classic_2,
    R.drawable.lp_classic_3,
    R.drawable.lp_classic_4,
    R.drawable.lp_classic_5,
    R.drawable.lp_classic_6,
    R.drawable.lp_classic_7,
    R.drawable.lp_classic_8,
    R.drawable.lp_classic_9,
    R.drawable.lp_classic_10,
    R.drawable.lp_classic_11,
    R.drawable.lp_classic_12,
    R.drawable.lp_classic_13,
    R.drawable.lp_classic_14,
    R.drawable.lp_classic_15,
    R.drawable.lp_classic_16,
    // Pop (17개)
    R.drawable.pop1,
    R.drawable.pop2,
    R.drawable.pop3,
    R.drawable.pop4,
    R.drawable.pop_5,
    R.drawable.pop_6,
    R.drawable.pop_7,
    R.drawable.ic_pop8,
    R.drawable.ic_pop9,
    R.drawable.ic_pop10,
    R.drawable.ic_pop11,
    R.drawable.ic_pop12,
    R.drawable.ic_pop13,
    R.drawable.ic_pop14,
    R.drawable.ic_pop15,
    R.drawable.ic_pop16,
    R.drawable.ic_pop17,
    // New Age (7개)
    R.drawable.newage_1,
    R.drawable.newage_2,
    R.drawable.newage_3,
    R.drawable.newage_4,
    R.drawable.newage_5,
    R.drawable.newage_6,
    R.drawable.newage_7,
    // OST (8개)
    R.drawable.ost1,
    R.drawable.ost2,
    R.drawable.ost3,
    R.drawable.ost4,
    R.drawable.ost5,
    R.drawable.ost6,
    R.drawable.ost7,
    R.drawable.ost8,
    // Kid Song (7개)
    R.drawable.kidsong_1,
    R.drawable.kidsong_2,
    R.drawable.kidsong_3,
    R.drawable.kidsong_4,
    R.drawable.kidsong_5,
    R.drawable.kidsong_6,
    R.drawable.kidsong_7,
    // CCM (5개)
    R.drawable.ccm_1,
    R.drawable.ccm_2,
    R.drawable.ccm_3,
    R.drawable.ccm_4,
    R.drawable.ccm_5
)

fun matchThumbnail(title: String): Int {
    val hash = title.hashCode()
    val index = Math.abs(hash % thumbnails.size)
    return thumbnails[index]
}

//fun matchThumbnail(title: String, genreId: Int): Int {
//    return when (genreId) {
//        1 -> getClassicThumbnail(title)
//        2 -> getPopThumbnail(title)
//        3 -> getNewAgeThumbnail(title)
//        4 -> getOstThumbnail(title)
//        5 -> getKidSongThumbnail(title)
//        6 -> getCcmThumbnail(title)
//        else -> getClassicThumbnail(title)
//    }
//}

fun getClassicThumbnail(title: String): Int {
    val hash = title.hashCode()
    val num = hash % 16
    return when (num) {
        0 -> R.drawable.lp_classic_1
        1 -> R.drawable.lp_classic_2
        2 -> R.drawable.lp_classic_3
        3 -> R.drawable.lp_classic_4
        4 -> R.drawable.lp_classic_5
        5 -> R.drawable.lp_classic_6
        6 -> R.drawable.lp_classic_7
        7 -> R.drawable.lp_classic_8
        8 -> R.drawable.lp_classic_9
        9 -> R.drawable.lp_classic_10
        10 -> R.drawable.lp_classic_11
        11 -> R.drawable.lp_classic_12
        12 -> R.drawable.lp_classic_13
        13 -> R.drawable.lp_classic_14
        14 -> R.drawable.lp_classic_15
        15 -> R.drawable.lp_classic_16
        else -> R.drawable.lp_classic_1
    }
}

fun getNewAgeThumbnail(title: String): Int {
    val hash = title.hashCode()
    val num = hash % 7
    return when (num) {
        0 -> R.drawable.newage_1
        1 -> R.drawable.newage_2
        2 -> R.drawable.newage_3
        3 -> R.drawable.newage_4
        4 -> R.drawable.newage_5
        5 -> R.drawable.newage_6
        6 -> R.drawable.newage_7
        else -> R.drawable.newage_1
    }
}

fun getPopThumbnail(title: String): Int {
    val hash = title.hashCode()
    val num = hash % 17
    return when (num) {
        0 -> R.drawable.pop1
        1 -> R.drawable.pop2
        2 -> R.drawable.pop3
        3 -> R.drawable.pop4
        4 -> R.drawable.pop_5
        5 -> R.drawable.pop_6
        6 -> R.drawable.pop_7
        7 -> R.drawable.ic_pop8
        8 -> R.drawable.ic_pop9
        9 -> R.drawable.ic_pop10
        10 -> R.drawable.ic_pop11
        11 -> R.drawable.ic_pop12
        12 -> R.drawable.ic_pop13
        13 -> R.drawable.ic_pop14
        14 -> R.drawable.ic_pop15
        15 -> R.drawable.ic_pop16
        16 -> R.drawable.ic_pop17
        else -> R.drawable.pop1
    }
}

fun getOstThumbnail(title: String): Int {
    val hash = title.hashCode()
    val num = hash % 8
    return when (num) {
        0 -> R.drawable.ost1
        1 -> R.drawable.ost2
        1 -> R.drawable.ost3
        2 -> R.drawable.ost4
        3 -> R.drawable.ost5
        4 -> R.drawable.ost6
        5 -> R.drawable.ost7
        6 -> R.drawable.ost8
        else -> R.drawable.ost1
    }
}

fun getKidSongThumbnail(title: String): Int {
    val hash = title.hashCode()
    val num = hash % 7
    return when (num) {
        0 -> R.drawable.kidsong_1
        1 -> R.drawable.kidsong_2
        2 -> R.drawable.kidsong_3
        3 -> R.drawable.kidsong_4
        4 -> R.drawable.kidsong_5
        5 -> R.drawable.kidsong_6
        6 -> R.drawable.kidsong_7
        else -> R.drawable.kidsong_1
    }
}

fun getCcmThumbnail(title: String): Int {
    val hash = title.hashCode()
    val num = hash % 5
    return when (num) {
        0 -> R.drawable.ccm_1
        1 -> R.drawable.ccm_2
        2 -> R.drawable.ccm_3
        3 -> R.drawable.ccm_4
        4 -> R.drawable.ccm_5
        else -> R.drawable.ccm_1
    }
}