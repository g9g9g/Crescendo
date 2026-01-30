package org.opensheetmusicdisplay.osmd.kotlin

import android.media.AudioFormat

/**
 * 오디오 처리 설정 (ONNX 모델과 일치하는 파라미터)
 */
object AudioConfig {
    const val SAMPLE_RATE = 16000        // 16kHz

    const val HOP_SAMPLES = 384          // 24ms @ 16kHz (41.67 fps) - 학습과 동일
    const val WIN_SAMPLES = 2048         // FFT window size (N_FFT)
    const val MEL_BINS    = 229          // 229 bins (학습과 동일)

    const val WINDOW_T_FRAMES = 5        // 5 프레임 = 0.12초 @ 41.67fps (5 * 24ms ≈ 120ms) - 초고속 추론

    // Mel 파라미터 (librosa 호환, 학습과 동일)
    const val FMIN = 50f
    const val FMAX = 8000f

    const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
}
