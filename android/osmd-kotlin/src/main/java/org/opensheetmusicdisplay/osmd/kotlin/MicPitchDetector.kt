package org.opensheetmusicdisplay.osmd.kotlin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.sqrt

/**
 * ONNX 기반 실시간 피아노 피치 검출기
 * 88건반 피아노 음을 동시에 검출 가능 (MIDI 21-108)
 *
 * @param context Android Context (ONNX 모델 로드용)
 * @param onNotes 검출된 음표 이벤트 콜백
 */
class MicPitchDetector(
    private val context: Context,
    private val onNotes: (List<NoteEvent>) -> Unit = {}
) {
    private val TAG = "MicPitchDetector"

    @Volatile private var running = false
    private var thread: Thread? = null
    private var recorder: AudioRecord? = null

    // ONNX 파이프라인
    private val melExtractor = MelFeatureExtractor()
    private val onnxEngine = OnnxInferenceEngine(context)

    // 모델 초기화 상태
    private var initialized = false

    /**
     * ONNX 모델 초기화 (start() 전에 호출 필요)
     */
    fun initialize(): Boolean {
        if (initialized) return true

        Log.i(TAG, "Initializing ONNX pitch detector...")
        if (!onnxEngine.initialize()) {
            Log.e(TAG, "Failed to initialize ONNX engine")
            return false
        }

        // 워밍업 (첫 추론 느림 방지)
        onnxEngine.warmup()

        initialized = true
        Log.i(TAG, "Initialization complete")
        return true
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (running) return
        if (!initialized) {
            Log.e(TAG, "Not initialized! Call initialize() first")
            return
        }

        // 권한 체크
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return
        }

        val hop = AudioConfig.HOP_SAMPLES
        val minBuf = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioConfig.CHANNEL,
            AudioConfig.ENCODING
        )
        val bufSize = maxOf(minBuf, hop * 4)

        // AudioRecord 생성 (UNPROCESSED 우선)
        try {
            recorder = buildRecorder(bufSize)
            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord")
                stop()
                return
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: RECORD_AUDIO permission denied", e)
            return
        }

        running = true
        try {
            recorder?.startRecording()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while starting recording", e)
            stop()
            return
        }

        thread = Thread { loop(hop) }.also {
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }

        Log.i(TAG, "Started pitch detection")
    }

    fun stop() {
        running = false
        try { thread?.join(300) } catch (_: Throwable) {}
        thread = null
        try { recorder?.stop() } catch (_: Throwable) {}
        try { recorder?.release() } catch (_: Throwable) {}
        recorder = null

        Log.i(TAG, "Stopped pitch detection")
    }

    /**
     * 리소스 해제 (사용 완료 후 호출)
     */
    fun close() {
        stop()
        onnxEngine.close()
        initialized = false
    }

    @SuppressLint("MissingPermission")
    private fun buildRecorder(bufferSize: Int): AudioRecord? {
        // UNPROCESSED → VOICE_RECOGNITION → MIC 폴백
        // 권한은 start() 함수에서 이미 체크됨
        val sources = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        for (src in sources) {
            try {
                val r = AudioRecord(
                    src,
                    AudioConfig.SAMPLE_RATE,
                    AudioConfig.CHANNEL,
                    AudioConfig.ENCODING,
                    bufferSize
                )
                if (r.state == AudioRecord.STATE_INITIALIZED) {
                    Log.i(TAG, "Using audio source: $src")
                    return r
                }
                r.release()
            } catch (_: Throwable) { /* try next */ }
        }
        return null
    }

    private fun loop(hop: Int) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        val rec = recorder ?: return
        val buf = ShortArray(hop)
        val floatBuf = FloatArray(hop)

        while (running) {
            val bytesRead = rec.read(buf, 0, hop, AudioRecord.READ_BLOCKING)
            if (bytesRead <= 0) continue

            // Convert to float [-1,1]
            for (i in 0 until bytesRead) {
                floatBuf[i] = buf[i] / 32768f
            }

            // Mel-spectrogram 추출
            val melSpec = melExtractor.push(floatBuf) ?: continue

            // ONNX 추론
            val onsetProbs = onnxEngine.runInference(melSpec) ?: continue

            // 음표 이벤트 추출
            val notes = extractNotes(onsetProbs)
            Log.d(TAG, "탐지된 음표들: $notes")

            if (notes.isNotEmpty()) {
                onNotes(notes)
            }
        }
    }

    /**
     * ONNX 출력을 음표 이벤트로 변환
     */
    private fun extractNotes(onsetProbs: FloatArray): List<NoteEvent> {
        // onsetProbs: [P * (T-1)] 형태 (P=88, T=WINDOW_T_FRAMES)
        val P = 88
        val T = onsetProbs.size / P

        // [P, T] → [T, P] 재구성
        val probs2d = Array(T) { t ->
            FloatArray(P) { p ->
                onsetProbs[p * T + t]
            }
        }

        // Sigmoid 적용 (logit → 확률)
        PostProcess.sigmoid2dInPlace(probs2d)

        // Frame 확률도 동일하게 사용 (간단화)
        val frameProbs = probs2d

        // Onset gating
        val keep = PostProcess.onsetGate(probs2d, frameProbs, poolK = 3)

        // 음표 이벤트 변환
        val hopMs = (AudioConfig.HOP_SAMPLES * 1000f) / AudioConfig.SAMPLE_RATE
        val notes = PostProcess.toNotes(keep, hopMs)

        // 로그용 함수
        if (notes.isEmpty()) {
            logSuppressedActivations(probs2d, frameProbs, keep, hopMs)
        }

        return notes
    }

    // 로그용 함수
    private fun logSuppressedActivations(
        onsetProb: Array<FloatArray>,
        frameProb: Array<FloatArray>,
        keep: Array<BooleanArray>,
        hopMs: Float
    ) {
        val T = onsetProb.size
        if (T == 0) return
        val P = onsetProb[0].size

        var bestOnset = 0f
        var bestOnsetMidi = -1
        var bestOnsetFrame = -1
        var bestFrame = 0f
        var bestFrameMidi = -1

        for (t in 0 until T) {
            for (p in 0 until P) {
                val midi = p + 21
                val onset = onsetProb[t][p]
                val frame = frameProb[t][p]
                if (onset > bestOnset) {
                    bestOnset = onset
                    bestOnsetMidi = midi
                    bestOnsetFrame = t
                }
                if (frame > bestFrame) {
                    bestFrame = frame
                    bestFrameMidi = midi
                }
            }
        }

        // 너무 낮은 값이면 노이즈로 간주하고 로그 생략
        if (bestOnset < 0.55f && bestFrame < 0.55f) return

        var longestKeepFrames = 0
        var longestKeepMidi = -1
        for (p in 0 until P) {
            var run = 0
            for (t in 0 until T) {
                if (keep[t][p]) {
                    run++
                    if (run > longestKeepFrames) {
                        longestKeepFrames = run
                        longestKeepMidi = p + 21
                    }
                } else {
                    run = 0
                }
            }
        }
        val longestKeepMs = longestKeepFrames * hopMs

        val onsetOk = bestOnset >= PostProcess.onsetStartTh
        val frameOk = bestFrame >= PostProcess.frameOnTh
        val durationOk = longestKeepMs >= PostProcess.minDurMs

        val reason = when {
            !onsetOk && !frameOk -> "onset/frame below thresholds"
            onsetOk && !frameOk -> "frame below ${PostProcess.frameOnTh}"
            !onsetOk && frameOk -> "onset below ${PostProcess.onsetStartTh}"
            onsetOk && frameOk && !durationOk -> "shorter than minDur ${PostProcess.minDurMs}ms"
            else -> "merged or filtered"
        }

        Log.d(
            TAG,
            String.format(
                Locale.US,
                "[Diag] bestOnset=%.3f(mid=%d,frame=%d) bestFrame=%.3f(mid=%d) longestKeep=%.1fms(mid=%d) reason=%s",
                bestOnset,
                bestOnsetMidi,
                bestOnsetFrame,
                bestFrame,
                bestFrameMidi,
                longestKeepMs,
                longestKeepMidi,
                reason
            )
        )
    }
}
