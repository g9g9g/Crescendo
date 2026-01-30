package org.opensheetmusicdisplay.osmd.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")

/**
 * Defines the OSMD interface, exposing both the composable [OSMDView]
 * and the control functions [play], [pause], [stop], [setCursorColor] & [setZoom].
 */
class OSMD {
    private lateinit var webview: WebView
    private lateinit var appContext: Context
    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    // ONNX 기반 피치 검출기 (88건반 동시 인식)
    private var onnxDetector: MicPitchDetector? = null

    // WAV 파일 녹음기 (연습 모드에서 파일 저장용)
    private var wavRecorder: WavFileRecorder? = null

    private var playbackActive: Boolean = false
    private var micWasOnBeforePlayback: Boolean = false

    // 녹음된 파일 Uri를 저장할 변수
    private var recordedFileUri: Uri? = null

    // Compose layer can subscribe to practice range changes coming from WebView (JS)
    private var practiceRangeListener: ((Int?, Int?) -> Unit)? = null

    // 미리듣기(ViewModel)용 재생 상태 리스너
    var playbackStateChangedListener: ((String) -> Unit)? = null

    // [!!] 하이라이트 상태 리스너 추가
    private var highlightStateListener: ((Boolean) -> Unit)? = null

    fun setPracticeRangeListener(listener: (Int?, Int?) -> Unit) {
        practiceRangeListener = listener
    }

    // [!!] 하이라이트 리스너 설정 함수 추가
    fun setHighlightStateListener(listener: (Boolean) -> Unit) {
        highlightStateListener = listener
    }

    private inner class JsBridge {
        @JavascriptInterface
        fun updatePracticeRange(start: Int, end: Int) {
            Log.d("AndroidOSMD","updatePracticeRange start=" + start + ", end=" + end)
            mainHandler.post {
                practiceRangeListener?.invoke(start, end)
            }
        }

        @JavascriptInterface
       fun clearPracticeRange() {
            Log.d("AndroidOSMD","clearPracticeRange")
            mainHandler.post {
                practiceRangeListener?.invoke(null, null)
            }
        }

        @JavascriptInterface
        fun updateHighlightState(isOn: Boolean) {
            Log.d("AndroidOSMD","updateHighlightState: $isOn")
            mainHandler.post {
                highlightStateListener?.invoke(isOn)
            }
        }

        @JavascriptInterface
        fun updateVisitedMeasures(min: Int, max: Int) {
            Log.d("AndroidOSMD","visited min="+min+", max="+max)
            mainHandler.post { visitedMeasuresListener?.invoke(min, max) }
        }

        /** Called from JS after sheet load + render + playback init is done. */
        @JavascriptInterface
        fun sheetReady() {
            Log.d("AndroidOSMD","sheetReady")
            mainHandler.post { pendingOnRender?.invoke() }
        }

        /** Playback state change notifications from JS: 'playing' | 'paused' | 'stopped' */
        @JavascriptInterface
        fun playbackStateChanged(state: String) {
            Log.d("AndroidOSMD","playbackStateChanged: $state")
            mainHandler.post {

                // 미리듣기 리스너 호출
                playbackStateChangedListener?.invoke(state)

                when(state) {
                    "playing" -> handlePlaybackStarted()
                    "paused", "stopped" -> handlePlaybackStoppedOrPaused()
                }
            }
        }

        /** Called from JS when an external pitch has been accepted (cursor advanced). */
        @JavascriptInterface
        fun pitchAccepted(midi: Int, cents: Float, measureNo: Int) {
            Log.d("AndroidOSMD","pitchAccepted midi=$midi cents=$cents measure=$measureNo")
            mainHandler.post { pitchAcceptedListener?.invoke(midi, cents, measureNo) }
        }
    }

    // Will be invoked when JS notifies that the sheet is ready
    private var pendingOnRender: (() -> Unit)? = null
    private var visitedMeasuresListener: ((Int, Int) -> Unit)? = null
    private var pitchAcceptedListener: ((Int, Float, Int) -> Unit)? = null

    fun setVisitedMeasuresListener(listener: (Int, Int) -> Unit) {
        visitedMeasuresListener = listener
    }

    /** Subscribe to pitch acceptance events (midi, cents, measure). */
    fun setPitchAcceptedListener(listener: (Int, Float, Int) -> Unit) {
        pitchAcceptedListener = listener
    }

    /** starts audio playback */
    fun play() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(startPlaybackScript, null)
            playbackActive = true
        }
    }

    /** pauses audio playback at the current position */
    fun pause() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(pausePlaybackScript, null)
            playbackActive = false
            handlePlaybackStoppedOrPaused()
        }
    }

    /** stops audio playback and resets to initial position */
    fun stop() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(stopPlaybackScript, null)
        }
    }

    /** sets the osmd cursor color */
    fun setCursorColor(color: String) {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(setCursorScript(color), null)
        }
    }

    /** sets the zoom scale */
    fun setZoom(scale: Float) {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(setZoomScaleScript(scale), null)
        }
    }

    // ===== Piano hand audio filter API =====
    enum class PianoHandMode { BOTH, LEFT, RIGHT }

    /** Keep both staves visible; filter audio to a hand. */
    fun setPianoHandMode(mode: PianoHandMode) {
        if (::webview.isInitialized) {
            val m = when(mode) {
                PianoHandMode.LEFT -> "left"
                PianoHandMode.RIGHT -> "right"
                else -> "both"
            }
            webview.evaluateJavascript(setPianoHandModeScript(m), null)
        }
    }

    // ===== Tempo & Metronome Native Bridge =====
    /** Set BPM (0~200 UI range, internally must be >0). override=true forces uniform tempo ignoring score markings. */
    fun setBpm(bpm: Int, overrideScore: Boolean = true) {
        if (!::webview.isInitialized) return
        val safe = bpm.coerceIn(0, 200) // allow 0 from UI; JS will ignore non-positive
        webview.evaluateJavascript("window.osmdIntegration && window.osmdIntegration.setBpm(${safe}, ${overrideScore});", null)
    }

    /** Query current effective BPM. Callback receives Int? or null if unavailable. */
    fun getCurrentBpm(callback: (Int?) -> Unit) {
        if (!::webview.isInitialized) { callback(null); return }
        webview.evaluateJavascript("(function(){return window.osmdIntegration && window.osmdIntegration.getCurrentBpm && window.osmdIntegration.getCurrentBpm();})()") { v ->
            try { callback(v?.trim()?.replace("\"", "")?.toInt()) } catch (_: Exception) { callback(null) }
        }
    }

    /** Get original sheet BPM (tempo at start before user overrides). */
    fun getOriginalBpm(callback: (Int?) -> Unit) {
        if (!::webview.isInitialized) { callback(null); return }
        webview.evaluateJavascript("(function(){return window.osmdIntegration && window.osmdIntegration.getOriginalBpm && window.osmdIntegration.getOriginalBpm();})()") { v ->
            try { callback(v?.trim()?.replace("\"", "")?.toInt()) } catch (_: Exception) { callback(null) }
        }
    }

    /** Reset BPM to sheet start tempo and re-enable tempo instructions. */
    fun resetTempoToSheetStart(callback: (Int?) -> Unit = {}) {
        if (!::webview.isInitialized) { callback(null); return }
        webview.evaluateJavascript("(function(){return window.osmdIntegration && window.osmdIntegration.resetTempoToSheetStart && window.osmdIntegration.resetTempoToSheetStart();})()") { v ->
            try { callback(v?.trim()?.replace("\"", "")?.toInt()) } catch (_: Exception) { callback(null) }
        }
    }

    /** Toggle metronome audible state. */
    fun setMetronomeEnabled(enabled: Boolean) {
        if (!::webview.isInitialized) return
        webview.evaluateJavascript("window.osmdIntegration && window.osmdIntegration.setMetronomeAudible(${enabled});", null)
    }

    /** Query metronome audible state. */
    fun getMetronomeEnabled(callback: (Boolean?) -> Unit) {
        if (!::webview.isInitialized) { callback(null); return }
        webview.evaluateJavascript("(function(){return window.osmdIntegration && window.osmdIntegration.getMetronomeAudible && window.osmdIntegration.getMetronomeAudible();})()") { v ->
            try { callback(v?.trim()?.replace("\"", "")?.toBooleanStrictOrNull()) } catch (_: Exception) { callback(null) }
        }
    }

    /** Enable/disable pre-count and optionally set number of measures. */
    fun setPrecount(enabled: Boolean, measures: Int = 1) {
        if (!::webview.isInitialized) return
        val m = measures.coerceAtLeast(1)
        webview.evaluateJavascript("window.osmdIntegration && window.osmdIntegration.setPrecount(${enabled}, ${m});", null)
    }

    /** Fetch composite tempo state (current, original, overrideActive, ignoreTempoInstructions). */
    fun getTempoState(callback: (TempoState?) -> Unit) {
        if (!::webview.isInitialized) { callback(null); return }
        webview.evaluateJavascript("(function(){ if(!window.osmdIntegration||!window.osmdIntegration.getTempoState) return null; return JSON.stringify(window.osmdIntegration.getTempoState()); })()") { json ->
            try {
                if (json == null || json == "null") { callback(null); return@evaluateJavascript }
                val obj = JSONObject(json)
                val state = TempoState(
                    current = if (obj.has("current") && !obj.isNull("current")) obj.getInt("current") else null,
                    original = if (obj.has("original") && !obj.isNull("original")) obj.getInt("original") else null,
                    overrideActive = obj.optBoolean("overrideActive", false),
                    ignoreTempoInstructions = obj.optBoolean("ignoreTempoInstructions", false)
                )
                callback(state)
            } catch (_: Exception) {
                callback(null)
            }
        }
    }

    // ===== Volume Bridge Wrappers =====
    /** Set master output gain as percent (0..200). 100 = normal. */
    fun setMasterVolumePercent(percent: Int) {
        if (!::webview.isInitialized) return
        val p = percent.coerceIn(0, 200)
        webview.evaluateJavascript("window.osmdIntegration && window.osmdIntegration.setMasterVolumePercent($p);", null)
    }
    /** Get master output gain percent (0..200). */
    fun getMasterVolumePercent(callback: (Int?) -> Unit) {
        if (!::webview.isInitialized) { callback(null); return }
        webview.evaluateJavascript("(function(){return window.osmdIntegration && window.osmdIntegration.getMasterVolumePercent && window.osmdIntegration.getMasterVolumePercent();})()") { v ->
            try { callback(v?.trim()?.replace("\"", "")?.toInt()) } catch (_: Exception) { callback(null) }
        }
    }
    /** Set metronome volume percent (0..100). */
    fun setMetronomeVolumePercent(percent: Int) {
        if (!::webview.isInitialized) return
        val p = percent.coerceIn(0, 100)
        webview.evaluateJavascript("window.osmdIntegration && window.osmdIntegration.setMetronomeVolumePercent($p);", null)
    }
    /** Get metronome volume percent (0..100). */
    fun getMetronomeVolumePercent(callback: (Int?) -> Unit) {
        if (!::webview.isInitialized) { callback(null); return }
        webview.evaluateJavascript("(function(){return window.osmdIntegration && window.osmdIntegration.getMetronomeVolumePercent && window.osmdIntegration.getMetronomeVolumePercent();})()") { v ->
            try { callback(v?.trim()?.replace("\"", "")?.toInt()) } catch (_: Exception) { callback(null) }
        }
    }

    // ===== Lightweight getters & navigation =====
    /** Get total measure count of current sheet. */
    fun getTotalMeasures(callback: (Int) -> Unit) {
        if (!::webview.isInitialized) { callback(0); return }
        webview.evaluateJavascript("(function(){return window.osmdIntegration && window.osmdIntegration.getTotalMeasures && window.osmdIntegration.getTotalMeasures();})()") { v ->
            try { callback(v?.trim()?.replace("\"", "")?.toInt() ?: 0) } catch (_: Exception) { callback(0) }
        }
    }
    /** Get current cursor measure number (1-based). */
    fun getCurrentMeasureNo(callback: (Int) -> Unit) {
        if (!::webview.isInitialized) { callback(1); return }
        webview.evaluateJavascript("(function(){return window.osmdIntegration && window.osmdIntegration.getCurrentMeasureNo && window.osmdIntegration.getCurrentMeasureNo();})()") { v ->
            try { callback(v?.trim()?.replace("\"", "")?.toInt() ?: 1) } catch (_: Exception) { callback(1) }
        }
    }
    /** Get current practice range (start,end) measure numbers or null if none. */
    fun getPracticeRange(callback: (Pair<Int,Int>?) -> Unit) {
        if (!::webview.isInitialized) { callback(null); return }
        webview.evaluateJavascript("(function(){ if(!window.osmdIntegration||!window.osmdIntegration.getPracticeRange) return null; return JSON.stringify(window.osmdIntegration.getPracticeRange()); })()") { json ->
            try {
                if (json == null || json == "null") { callback(null); return@evaluateJavascript }
                val obj = JSONObject(json)
                val s = obj.getInt("start")
                val e = obj.getInt("end")
                callback(Pair(s,e))
            } catch (_: Exception) { callback(null) }
        }
    }
    /** Programmatically jump to a measure (1-based). */
    fun goToMeasure(no: Int) {
        if (!::webview.isInitialized) return
        val n = no.coerceAtLeast(1)
        webview.evaluateJavascript("window.osmdIntegration && window.osmdIntegration.goToMeasure(${n});", null)
    }

    /** Force visited range to a specific measure (min=max=no). Useful after initial jump. */
    fun resetVisitedToMeasure(no: Int) {
        if (!::webview.isInitialized) return
        val n = no.coerceAtLeast(1)
        webview.evaluateJavascript("window.osmdIntegration && window.osmdIntegration.resetVisitedTo(${n});", null)
    }

    // ===== Mic matching configuration =====
    /** Minimum ms between accepted notes to avoid multiple advances on sustained tones. */
    fun setMicAdvanceCooldownMs(ms: Int) {
        if (!::webview.isInitialized) return
        val v = ms.coerceIn(0, 2000)
        webview.evaluateJavascript("window.osmdIntegration && window.osmdIntegration.setMicAdvanceCooldownMs(${v});", null)
    }

    fun setRestAutoAdvanceEnabled(enabled: Boolean) {
        if (!::webview.isInitialized) return
        webview.evaluateJavascript("window.osmdIntegration && window.osmdIntegration.setRestAutoAdvanceEnabled(${enabled});", null)
    }

    data class TempoState(
        val current: Int?,
        val original: Int?,
        val overrideActive: Boolean,
        val ignoreTempoInstructions: Boolean
    )

    fun toggleHighlight() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(toggleHighlightScript, null)
        }
    }

    /*
    setPracticeRangeScript(start,end)는 “숫자 입력으로 바로 구간 지정” 같은 프로그래매틱한 설정에 사용하세요.
    현재 Compose UI에서는 Pick/Back/Clear만 쓰고 있어서 IDE가 “사용처 없음”으로 보일 수 있어요.
     */
    /** sets the practice range by measures (1-based) and applies playback start */
    fun setPracticeRange(startMeasure: Int, endMeasure: Int) {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(setPracticeRangeScript(startMeasure, endMeasure), null)
        }
    }

    /** clears the practice range */
    fun clearPracticeRange() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(clearPracticeRangeScript, null)
        }
    }

    /** moves playback cursor back to practice start */
    fun backToPracticeStart() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(backToPracticeStartScript, null)
        }
    }

    /** starts interactive range picking in WebView (tap start, then end) */
    fun startPracticePick() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(startPracticePickScript, null)
        }
    }

    /** stops interactive range picking */
    fun stopPracticePick() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(stopPracticePickScript, null)
        }
    }

    /** enable/disable Repeat behavior (range loops if set; otherwise loops whole piece) */
    fun setRepeatEnabled(enabled: Boolean) {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(setRepeatEnabledScript(enabled), null)
        }
    }

    /** toggle Repeat behavior */
    fun toggleRepeat() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(toggleRepeatScript, null)
        }
    }

    // (Removed) WebView mic control; Android uses native mic pipeline only.

    // ===== Native mic pipeline (preferred on Android) =====
    /** Start native AudioRecord + autocorrelation and forward detected pitches to WebView */
    @SuppressLint("MissingPermission")
    fun startNativeMic(isPracticeMode: Boolean) {
        if (onnxDetector != null || wavRecorder != null) return
        if (playbackActive) {
            // Defer mic start until playback stops
            micWasOnBeforePlayback = true
            Log.d("Mic", "Deferred start while playback active")
            return
        }

        // 녹음 시작 시, 이전 Uri는 초기화
        recordedFileUri = null

        // 1. ONNX 기반 피치 검출기 시작 (88건반 동시 인식)
        onnxDetector = MicPitchDetector(
            context = appContext,
            onNotes = { notes ->
                // 검출된 음표들을 WebView에 전달
                mainHandler.post {
                    if (::webview.isInitialized) {
                        notes.forEach { note ->
                            val midi = note.pitch
                            val cents = 0f // ONNX는 정확한 피치이므로 cents는 0
                            try {
                                Log.d("Mic", "ONNX detected: midi=${midi} (${note.tOn}s - ${note.tOff}s)")
                            } catch (_: Exception) {}
                            webview.evaluateJavascript(externalPitchScript(midi, cents), null)
                        }
                    }
                }
            }
        )

        // ONNX 모델 초기화 및 시작
        if (onnxDetector?.initialize() == true) {
            onnxDetector?.start()
            Log.d("Mic", "ONNX pitch detector started")
        } else {
            Log.e("Mic", "Failed to initialize ONNX detector")
            onnxDetector = null
        }

        // 2. WAV 파일 녹음 시작 (평가 모드에서만 파일 저장)
        if (!isPracticeMode) {  //  녹음 되지 않는 현상 제거!!
            wavRecorder = WavFileRecorder(
                context = appContext,
            )
            wavRecorder?.start(isPracticeMode = false)
            Log.d("Mic", "WAV recorder started for evaluation mode")
        }

        // Keep rest auto-advance active for both practice and evaluation modes.
        setRestAutoAdvanceEnabled(true)
    }

    /** Stop native mic detection */
    fun stopNativeMic() {
        // 1. ONNX 피치 검출기 정지
        onnxDetector?.stop()
        onnxDetector?.close()
        onnxDetector = null
        Log.d("Mic", "ONNX pitch detector stopped")

        // 2. WAV 녹음기 정지 및 파일 URI 저장
        recordedFileUri = wavRecorder?.stop()
        wavRecorder = null
        if (recordedFileUri != null) {
            Log.d("Mic", "WAV file saved: $recordedFileUri")
        }

        // User explicitly turned mic off; don't auto-restore on pause
        micWasOnBeforePlayback = false

        setRestAutoAdvanceEnabled(false)
    }

    // ViewModel이 Uri를 가져갈 수 있도록 메서드 추가
    fun getRecordedFileUri(): Uri? {
        // stopNativeMic()이 호출될 때 'recordedFileUri'가 설정되었기를 기대함
        return recordedFileUri
    }

    private fun handlePlaybackStarted() {
        playbackActive = true
        if (onnxDetector != null || wavRecorder != null) {
            // Remember and pause mic while playing
            micWasOnBeforePlayback = true
            try {
                // ONNX 검출기 정지
                onnxDetector?.stop()
                onnxDetector?.close()
                onnxDetector = null

                // WAV 녹음기 정지 및 Uri 저장
                recordedFileUri = wavRecorder?.stop()
                wavRecorder = null
            } catch (e: Throwable) {
                Log.e("Mic", "Error stopping mic during playback start", e)
            }
            Log.d("Mic", "Paused mic due to playback start")
        }

        setRestAutoAdvanceEnabled(false)
    }

    private fun handlePlaybackStoppedOrPaused() {
        val wasPlaying = playbackActive
        playbackActive = false
        if (micWasOnBeforePlayback && onnxDetector == null && wavRecorder == null) {
            // Restore mic after playback pause/stop
            micWasOnBeforePlayback = false
            startNativeMic(isPracticeMode = true)
            Log.d("Mic", "Restored mic after playback stop/pause")
        }
    }

    /**
     * The composable OSMDView rendering a music sheet.
     *
     * @param musicXML the path to the music sheet file (.xml or .mxl inside assets folder)
     * @param options optional list of OSMD options (see [IOSMDTypes](https://github.com/opensheetmusicdisplay/osmd-types-player))
     * @param onRender optional function callback to be after render (i.e., for loading indicators etc.)
     */
    @Composable
    fun OSMDView(musicXML: String, options: JSONObject? = null,
                 hasLoadStarted: Boolean,
                 setLoadStarted: () -> Unit,
                 onRender: (() -> Unit)? = null) {

        AndroidView(factory = { c ->

            this.appContext = c.applicationContext

            // Enable Chrome remote debugging for WebView (use chrome://inspect)
            WebView.setWebContentsDebuggingEnabled(true)
            return@AndroidView WebView(c).apply {
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
                // Expose JS bridge early so it's available as soon as the page loads
                addJavascriptInterface(JsBridge(), "AndroidOSMD")

                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(c)).build()

                val wvClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView, request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (view != null) {
                            webview = view
                            var js = ""
                            if (options != null) {
                                js += setOptionsScript(options)
                            }
                            webview.evaluateJavascript(
                                js + loadMusicXMLScript(musicXML), null
                            )
                            // Defer onRender until JS calls AndroidOSMD.sheetReady()
                            pendingOnRender = onRender
                        }
                    }
                }

                webViewClient = wvClient
            }
        }, update = { webView ->
            val targetUrl = "https://appassets.androidplatform.net/assets/index.html"

            Log.d("Verify", "OSMDView Update: hasLoadStarted=$hasLoadStarted, URL=${webView.url}")

            // [수정] 로드를 "시작"한 적이 없다면 로드하고, 상태를 true로 변경합니다.
            if (!hasLoadStarted) {
                webView.loadUrl(targetUrl)
                setLoadStarted()
            }
        })
    }

    /** Attach console note logger to WebView (prints Expected Onset / Under Cursor to DevTools console). */
    fun enableNoteConsoleLogs() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(attachNoteLoggerScript, null)
        }
    }

    /** Toggle console note logs on. */
    fun noteLogsOn() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(enableNoteLogsScript, null)
        }
    }

    /** Toggle console note logs off. */
    fun noteLogsOff() {
        if (::webview.isInitialized) {
            webview.evaluateJavascript(disableNoteLogsScript, null)
        }
    }

    /**
     * [신규] 악보를 렌더링하지 않고 오디오만 로드하고 즉시 재생
     * (미리듣기 전용)
     *
     * @param musicXML 로드 및 재생할 MusicXML 문자열
     */
    fun loadMusicXMLWithoutRendering(musicXML: String) {
        if (::webview.isInitialized) {
            // 단계 1에서 만든 새 스크립트를 호출합니다.
            webview.evaluateJavascript(loadAndPlayMusicXMLScript(musicXML), null)
        } else {
            Log.w("OSMD", "loadMusicXMLWithoutRendering called, but WebView is not initialized.")
        }
    }

    fun destroy() {
        if (::webview.isInitialized) {
            // 모든 JS 콜백 리스너 참조 해제 (메모리 누수 방지)
            practiceRangeListener = null
            visitedMeasuresListener = null
            pitchAcceptedListener = null

            playbackStateChangedListener = null

            // WebView 정리
            webview.stopLoading()
            webview.destroy()
        }
        // 네이티브 마이크가 켜져있었다면 확실히 종료
        stopNativeMic()

        // [!!] 15. Uri 참조 해제
        recordedFileUri = null
    }
}
