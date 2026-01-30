package org.opensheetmusicdisplay.osmd.kotlin

import kotlin.math.*

/**
 * 실시간 Mel-Spectrogram 추출기
 * PCM 오디오를 누적하여 [T=21, M=229] 크기의 log-mel 스펙트로그램 반환
 */
class MelFeatureExtractor(
    private val sampleRate: Int = AudioConfig.SAMPLE_RATE,
    private val win: Int = AudioConfig.WIN_SAMPLES,
    private val hop: Int = AudioConfig.HOP_SAMPLES,
    private val melBins: Int = AudioConfig.MEL_BINS,
    private val windowT: Int = AudioConfig.WINDOW_T_FRAMES,
) {
    private val frameBuffer = FloatArray(win)
    private var rb = FloatArray(win + hop * windowT * 2) // 여유 버퍼
    private var rbSize = 0

    private val hann = FloatArray(win) { i ->
        (0.5f - 0.5f * cos(2.0 * Math.PI * i / (win - 1))).toFloat()
    }
    private val fftSize = 1.shl(ceil(log2(win.toDouble())).toInt()) // 최소 2048
    private val fftRe = FloatArray(fftSize)
    private val fftIm = FloatArray(fftSize)
    private val melFilter = buildMelFilterBank(melBins, fftSize, sampleRate)
    private val powSpec = FloatArray(fftSize / 2 + 1)
    private val curMels = ArrayList<FloatArray>(windowT)

    private fun log2(x: Double) = ln(x) / ln(2.0)

    /**
     * PCM 샘플을 push하고, 충분히 쌓이면 [T, M] mel-spectrogram 반환
     * @param pcm FloatArray 범위 [-1.0, 1.0]
     * @return Array<FloatArray>? - [T=21, M=229] 또는 null (아직 준비 안됨)
     */
    fun push(pcm: FloatArray): Array<FloatArray>? {
        // 링버퍼에 누적
        ensureCapacity(rbSize + pcm.size)
        System.arraycopy(pcm, 0, rb, rbSize, pcm.size)
        rbSize += pcm.size

        // 프레임 단위로 처리
        while (rbSize >= win) {
            // frame = rb[0..win)
            for (i in 0 until win) frameBuffer[i] = rb[i] * hann[i]
            // FFT → 파워스펙트럼
            powerSpectrum(frameBuffer, fftRe, fftIm, powSpec)

            // 멜 필터 적용 (229 bins, power spectrum)
            val mel229 = FloatArray(melBins)
            for (m in 0 until melBins) {
                var s = 0f
                val filt = melFilter[m]
                for (k in 0 until filt.size step 2) {
                    val bin = filt[k].toInt()
                    val w   = filt[k + 1]
                    s += w * powSpec[bin]
                }
                // Power to dB (librosa 호환)
                mel229[m] = powerToDb(s)
            }

            // 학습과 동일: 정규화 없이 raw dB 사용
            curMels.add(mel229)

            // 윈도우 준비 다 되면 [T,M] 반환
            if (curMels.size == windowT) {
                val out = Array(windowT) { t -> curMels[t] }
                curMels.clear()
                // 링버퍼에서 hop만큼 shift
                shiftLeft(rb, hop)
                rbSize -= hop
                return out
            }
            // 링버퍼 hop만큼 앞으로 당김
            shiftLeft(rb, hop)
            rbSize -= hop
        }
        return null
    }

    private fun ensureCapacity(need: Int) {
        if (need <= rb.size) return
        var ns = rb.size
        while (ns < need) ns *= 2
        rb = rb.copyOf(ns)
    }

    private fun shiftLeft(a: FloatArray, n: Int) {
        if (n <= 0) return
        System.arraycopy(a, n, a, 0, a.size - n)
    }

    // Cooley-Tukey radix-2 FFT (실수입력 → 복소 FFT)
    private fun powerSpectrum(frame: FloatArray, re: FloatArray, im: FloatArray, out: FloatArray) {
        java.util.Arrays.fill(re, 0f)
        java.util.Arrays.fill(im, 0f)
        for (i in frame.indices) re[i] = frame[i]
        fft(re, im) // in-place
        val n = re.size
        val half = n / 2
        for (k in 0..half) {
            val rr = re[k]; val ii = im[k]
            out[k] = rr * rr + ii * ii
        }
    }

    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        // bit-reversal
        var j = 0
        for (i in 1 until n - 1) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = (-2.0 * Math.PI / len).toFloat()
            val wlenRe = cos(ang)
            val wlenIm = sin(ang)
            for (i in 0 until n step len) {
                var wr = 1f; var wi = 0f
                val half = len shr 1
                for (k in 0 until half) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + half] * wr - im[i + k + half] * wi
                    val vIm = re[i + k + half] * wi + im[i + k + half] * wr
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + half] = uRe - vRe
                    im[i + k + half] = uIm - vIm
                    val nwr = wr * wlenRe - wi * wlenIm
                    wi = wr * wlenIm + wi * wlenRe
                    wr = nwr
                }
            }
            len = len shl 1
        }
    }

    // Mel 필터뱅크 (삼각 필터, HTK 방식). 압축 저장: [bin, weight, bin, weight, ...]
    private fun buildMelFilterBank(melBins: Int, fftSize: Int, sr: Int): Array<FloatArray> {
        val nFft = fftSize
        val nSpec = nFft / 2 + 1
        val fMin = AudioConfig.FMIN.toDouble()
        val fMax = AudioConfig.FMAX.toDouble()

        // HTK mel scale (librosa htk=True와 동일)
        fun hz2mel(f: Double) = 2595.0 * ln(1 + f / 700.0)
        fun mel2hz(m: Double) = 700.0 * (exp(m / 2595.0) - 1.0)

        val melMin = hz2mel(fMin)
        val melMax = hz2mel(fMax)
        val melPts = DoubleArray(melBins + 2) { i ->
            melMin + (melMax - melMin) * i / (melBins + 1)
        }
        val hzPts = DoubleArray(melPts.size) { mel2hz(melPts[it]) }
        val bin = IntArray(hzPts.size) { i ->
            floor((nFft + 1) * hzPts[i] / sr).toInt().coerceIn(0, nSpec - 1)
        }

        val filters = Array(melBins) { FloatArray(0) }
        for (m in 1..melBins) {
            val left = bin[m - 1]; val center = bin[m]; val right = bin[m + 1]
            val entries = ArrayList<Float>((right - left) * 2)
            for (k in left until center) {
                val w = (k - left).toFloat() / max(1, center - left).toFloat()
                if (k in 0 until nSpec && w > 0f) {
                    entries.add(k.toFloat()); entries.add(w)
                }
            }
            for (k in center..right) {
                val w = (right - k).toFloat() / max(1, right - center).toFloat()
                if (k in 0 until nSpec && w > 0f) {
                    entries.add(k.toFloat()); entries.add(w)
                }
            }
            filters[m - 1] = entries.toFloatArray()
        }
        return filters
    }

    // Power to dB (librosa 호환)
    private fun powerToDb(power: Float): Float {
        val db = 10.0f * log10(max(1e-10f, power))
        return db.coerceAtLeast(-80f)  // 최소 -80dB
    }
}
