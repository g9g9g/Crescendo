package org.opensheetmusicdisplay.osmd.kotlin

import kotlin.math.exp
import kotlin.math.max

/**
 * 음표 이벤트 (검출된 음)
 * @param pitch MIDI 음높이 (21-108, 88개 피아노 건반)
 * @param tOn 시작 시간(초)
 * @param tOff 종료 시간(초)
 */
data class NoteEvent(val pitch: Int, val tOn: Float, val tOff: Float)

/**
 * ONNX 모델 출력을 음표 이벤트로 변환
 */
object PostProcess {
    // 시작/유지/종료 히스테리시스 임계값
    var onsetStartTh = 0.6f   // 온셋 시작 임계값
    var frameOnTh    = 0.6f   // 프레임 유지 임계값
    var frameOffTh   = 0.4f   // 프레임 종료 임계값

    // TODO: 본디 값은 50f, 30f였는데 이거 좀 줄이니까 잘 되는 거 같아요
    var minDurMs     = 20f    // 최소 노트 길이 (ms)
    var mergeGapMs   = 10f    // 같은 음에서 간격이 짧으면 병합 (ms)

    private fun sigmoid(x: Float) = 1f / (1f + exp(-x))

    /**
     * 2D 배열에 sigmoid 적용 (in-place)
     */
    fun sigmoid2dInPlace(logits: Array<FloatArray>) {
        val T = logits.size
        if (T == 0) return
        val P = logits[0].size
        for (t in 0 until T) {
            val row = logits[t]
            for (p in 0 until P) row[p] = sigmoid(row[p])
        }
    }

    /**
     * Onset gating: onset과 frame 확률을 결합하여 음표 활성화 판단
     * @param onsetProb [T][P] onset 확률 (0..1)
     * @param frameProb [T][P] frame 확률 (0..1)
     * @param poolK onset 주변 max-pooling 커널 크기 (홀수)
     * @return [T][P] 음표 활성화 boolean 배열
     */
    fun onsetGate(
        onsetProb: Array<FloatArray>,
        frameProb: Array<FloatArray>,
        poolK: Int = 5
    ): Array<BooleanArray> {
        val T = frameProb.size
        require(T > 0) { "Empty probability array" }
        val P = frameProb[0].size
        val keep = Array(T) { BooleanArray(P) }
        val pad = (poolK - 1) / 2

        for (p in 0 until P) {
            for (t in 0 until T) {
                // 주변 온셋 존재 여부 확인
                var hasOnset = false
                var k = -pad
                while (k <= pad && !hasOnset) {
                    val tt = t + k
                    if (tt in 0 until T && onsetProb[tt][p] >= onsetStartTh) hasOnset = true
                    k++
                }
                // 히스테리시스: 유지/종료는 frameOn/Off로 구분
                val f = frameProb[t][p]
                keep[t][p] = hasOnset && (f >= frameOnTh)
            }
        }

        // 간단한 침식: 바로 다음 프레임이 off면 순간 스파이크 제거
        for (p in 0 until P) {
            for (t in 1 until T-1) {
                if (keep[t][p] && !keep[t-1][p] && !keep[t+1][p]) {
                    keep[t][p] = frameProb[t][p] >= max(frameOnTh, 0.7f)
                }
            }
        }

        // 오프 경계: frameOffTh 아래면 확실히 끊기도록
        for (p in 0 until P) {
            for (t in 0 until T) {
                if (keep[t][p] && frameProb[t][p] < frameOffTh) keep[t][p] = false
            }
        }
        return keep
    }

    /**
     * Boolean 배열을 NoteEvent 리스트로 변환
     * @param keep [T][P] 음표 활성화 배열
     * @param hopMs 프레임 간격 (ms)
     * @return 검출된 음표 이벤트 리스트
     */
    fun toNotes(keep: Array<BooleanArray>, hopMs: Float): List<NoteEvent> {
        val T = keep.size
        val P = keep[0].size
        val notes = mutableListOf<NoteEvent>()

        for (p in 0 until P) {
            var t0 = -1
            for (t in 0 until T) {
                val on = keep[t][p]
                if (on && t0 < 0) t0 = t
                val isLast = (t == T - 1)
                if ((!on || isLast) && t0 >= 0) {
                    val t1 = if (on && isLast) t else t - 1
                    val startMs = t0 * hopMs
                    val endMs   = (t1 + 1) * hopMs
                    if (endMs - startMs >= minDurMs) {
                        notes += NoteEvent(
                            pitch = p + 21,  // MIDI 21-108 (88 keys)
                            tOn = startMs / 1000f,
                            tOff = endMs / 1000f
                        )
                    }
                    t0 = -1
                }
            }
        }

        // 같은 피치 병합 (간격이 mergeGapMs 이하면 하나로)
        if (mergeGapMs > 0f && notes.size > 1) {
            val merged = mutableListOf<NoteEvent>()
            val byPitch = notes.groupBy { it.pitch }.toMutableMap()
            for ((pitch, list) in byPitch) {
                val sorted = list.sortedBy { it.tOn }
                var cur = sorted.first()
                for (i in 1 until sorted.size) {
                    val nxt = sorted[i]
                    val gapMs = (nxt.tOn - cur.tOff) * 1000f
                    if (gapMs <= mergeGapMs) {
                        cur = cur.copy(tOff = max(cur.tOff, nxt.tOff))
                    } else {
                        merged += cur
                        cur = nxt
                    }
                }
                merged += cur
            }
            return merged.sortedBy { it.tOn }
        }
        return notes.sortedBy { it.tOn }
    }
}
