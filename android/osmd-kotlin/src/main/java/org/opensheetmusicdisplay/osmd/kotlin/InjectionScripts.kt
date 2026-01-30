package org.opensheetmusicdisplay.osmd.kotlin

import org.json.JSONObject

/*
요점만 정리하면:

fun: 호출 시점에 실행되는 함수
    목적: 파라미터를 받아서 “동적으로” 문자열(JS 스니펫)을 만들어낼 때 사용.
    예: setOptions(options), setZoomScale(zoom), setPracticeRange(start, end)
    각 호출 때마다 전달된 값들을 문자열에 삽입해 새 문자열을 반환.
    특징: 런타임 값 사용 가능, 문자열 템플릿/가공/이스케이프 처리 등 로직 포함 가능.


const val: “컴파일 타임 상수” 문자열
    목적: 내용이 고정된 JS 스니펫을 재사용할 때 사용.
    예: startPlayback, pausePlayback, clearPracticeRange 등
    항상 동일한 스크립트 내용을 그대로 WebView에 주입.
    특징:
    컴파일 타임에 값이 확정되는 상수여야 함(프리미티브/문자열 리터럴 등).
    파라미터 삽입이나 런타임 계산 불가(다른 상수만 참조 가능).
    최적화(inline) 가능, 오버헤드 거의 없음.
    톱레벨/객체(object)/companion object 안에서만 선언 가능.

언제 무엇을 쓰나
    입력 값에 따라 스크립트가 달라져야 하면 fun으로 함수로 만들어 호출 시 생성.
    항상 같은 스크립트를 넣을 거면 const val로 상수로 두고 그대로 사용.
 */


/** sets any supported IOSMDOptions   */
fun setOptionsScript(options: JSONObject): String {
    return """
       osmd.setOptions(${options}); 
    """
}

/** loads the actual music xml */
fun loadMusicXMLScript(musicXML: String): String {
    return """
    try {
      if (osmd.drawer?.backend?.ctx != undefined) {
        osmd.clear();
      }
      osmd
        .load("$musicXML")
        .then(() => {
          osmd.initPlaybackManager();
          // osmd.render()를 return하여 Promise 체인을 연결합니다.
          
          // [검증 로그 1] 렌더링 "시작"
          console.log('[Verify] 1. osmd.render() CALLED');
          return osmd.render(); 
        })
        .then(() => {
          // --- 이 .then() 블록은 렌더링이 완료된 후 실행됩니다 ---
          
          try { 
            window.__practiceRange && window.__practiceRange.attach && window.__practiceRange.attach(); 
          } catch(e) { 
            console.warn('[PracticeRange] attach failed', e);
          }
          
          // [!!] 하이라이트 새로고침 코드 추가 [!!]
          try {
            if (window.__practiceRange && window.__practiceRange.refreshHighlight) {
              window.__practiceRange.refreshHighlight();
              console.log('[PracticeRange] Highlight refreshed after load.');
            }
          } catch(e) { console.warn('[PracticeRange] refreshHighlight failed after load', e); }
          // [!!] 여기까지 추가 [!!]

          // ▼▼▼ sheetReady() 호출을 여기로 이동 ▼▼▼
          try { 
            if (window.AndroidOSMD && window.AndroidOSMD.sheetReady) { 
              // [검증 로그 2] 렌더링 "완료"
              console.log('[Verify] 2. osmd.render() FINISHED');
              // [검증 로그 3] 안드로이드 호출 "직전"
              console.log('[Verify] 3. sheetReady() CALLED');
              window.AndroidOSMD.sheetReady(); 
            } 
          } catch(e) { 
            console.warn('[loadScript] sheetReady notify failed', e); 
          }
          // --- 여기까지 추가 ---
        })
        .catch((error) => {
          console.log(error);
        });
    } catch (error) {
        console.log(error);
    }
    """
}

/**
 * 렌더링 없이 XML을 로드하고 즉시 재생 (미리듣기용)
 */
fun loadAndPlayMusicXMLScript(musicXML: String): String {
    return """
    try {
      // 1. 이전 악보가 있다면 지웁니다.
      if (osmd.drawer?.backend?.ctx != undefined) {
        osmd.clear();
      }
      
      // 2. 새 악보를 로드합니다.
      osmd
        .load("$musicXML")
        .then(() => {
          // 3. [핵심] 렌더링(osmd.render()) 없이 재생 관리자만 초기화합니다.
          osmd.initPlaybackManager(); 
          console.log('[Preview] initPlaybackManager() called.');

          // 4. [핵심] 즉시 재생을 시작합니다.
          osmd.PlaybackManager.playDummySound();
          osmd.PlaybackManager.play();
          console.log('[Preview] Playback STARTED.');
        })
        .catch((error) => {
          console.error('[Preview] Load/Play Error:', error);
        });
    } catch (error) {
        console.error('[Preview] Try-Catch Error:', error);
    }
    """
}


const val startPlaybackScript = """
osmd.PlaybackManager.playDummySound();
osmd.PlaybackManager.play();
"""

const val pausePlaybackScript = """
osmd.PlaybackManager.pause();
"""

// 무조건 맨 처음으로 돌아가려면 setPlaybackStart()를 사용함.
const val stopPlaybackScript = """
osmd.PlaybackManager.pause();
osmd.PlaybackManager.setPlaybackStart();
"""

// 클릭된 지점으로 돌아가려면 reset()을 사용함.
const val stopAndGoToClickScript = """
osmd.PlaybackManager.pause();
osmd.PlaybackManager.reset();
"""
// 어... 그러면 반복할 지점은 두 군데를 클릭하는데 그 두 지점을 어떻게 기록하지?
// 나중에 보통 반복의 시작을 먼저 클릭하고 다음까지 재생하는데
// 그리고 두 번째로 찍은 마디로 가면 클릭이 멈춰야 하는데 이걸 어떻게 하지????


fun setCursorScript(color: String): String {
    return """
    osmd.cursor.CursorOptions.color = "$color";
    osmd.cursor.update();
    """
}

fun setZoomScaleScript(zoom: Float): String {
    return """
    osmd.Zoom = $zoom;
    osmd.render(); 
    
    // [!!] 수정: .then()을 제거하고 render() 다음 줄에서 바로 호출합니다.
    try {
      if (window.__practiceRange && window.__practiceRange.refreshHighlight) {
        window.__practiceRange.refreshHighlight();
        console.log('[PracticeRange] Highlight refreshed after zoom.');
      }
    } catch(e) { console.warn('[PracticeRange] refreshHighlight failed after zoom', e); }
    """
}

/** Ask page to attach note logger if helper exists (minimal, no setup). */
const val attachNoteLoggerScript = """
(function(){
  try{
    if (window.osmdIntegration && typeof window.osmdIntegration.attachNoteLogger === 'function') {
      window.osmdIntegration.attachNoteLogger();
    } else {
      console.log('[NoteLog] attachNoteLogger: osmdIntegration not ready');
    }
  } catch(e) { console.warn('[NoteLog] attach error:', e); }
})();
"""

const val enableNoteLogsScript = """
try { window.osmdIntegration = window.osmdIntegration || {}; window.osmdIntegration.noteLogEnabled = true; console.log('[NoteLog] enabled'); } catch(e) { console.warn('[NoteLog]', e); }
"""

const val disableNoteLogsScript = """
try { if (window.osmdIntegration) { window.osmdIntegration.noteLogEnabled = false; console.log('[NoteLog] disabled'); } } catch(e) { console.warn('[NoteLog]', e); }
"""

// ===== Practice Range control (Kotlin -> WebView) =====
fun setPracticeRangeScript(start: Int, end: Int): String {
  return """
  try { window.__practiceRange && window.__practiceRange.set && window.__practiceRange.set($start, $end); } catch(e) { console.warn('[PracticePick] set error', e); }
  """
}

const val clearPracticeRangeScript = """
try { window.__practiceRange && window.__practiceRange.clear && window.__practiceRange.clear(); } catch(e) { console.warn('[PracticePick] clear error', e); }
"""

const val backToPracticeStartScript = """
try { window.__practiceRange && window.__practiceRange.back && window.__practiceRange.back(); } catch(e) { console.warn('[PracticePick] back error', e); }
"""

const val startPracticePickScript = """
try { window.__practiceRange && window.__practiceRange.startPick && window.__practiceRange.startPick(); } catch(e) { console.warn('[PracticePick] startPick error', e); }
"""

const val stopPracticePickScript = """
try { window.__practiceRange && window.__practiceRange.stopPick && window.__practiceRange.stopPick(); } catch(e) { console.warn('[PracticePick] stopPick error', e); }
"""
// [!!] 이 상수를 추가하세요
const val toggleHighlightScript = """
try { window.__practiceRange && window.__practiceRange.toggleHighlight && window.__practiceRange.toggleHighlight(); } catch(e) { console.warn('[PracticeRange] toggleHighlight error', e); }
"""

// ===== Repeat control (Kotlin -> WebView) =====
fun setRepeatEnabledScript(enabled: Boolean): String {
    val flag = if (enabled) "true" else "false"
    return """
    (function(){
      try {
        if (window.__practiceRange && typeof window.__practiceRange.setRepeatEnabled === 'function') {
          window.__practiceRange.setRepeatEnabled($flag);
        } else {
          console.log('[Repeat] setRepeatEnabled: helper not ready');
        }
      } catch(e) { console.warn('[Repeat] setRepeatEnabled error:', e); }
    })();
    """
}

const val toggleRepeatScript = """
(function(){
  try {
    if (window.__practiceRange && typeof window.__practiceRange.toggleRepeat === 'function') {
      window.__practiceRange.toggleRepeat();
    } else {
      console.log('[Repeat] toggleRepeat: helper not ready');
    }
  } catch(e) { console.warn('[Repeat] toggle error:', e); }
})();
"""

// (Removed) WebView mic control scripts — Android uses native mic pipeline only.

// Feed external (native) pitch to WebView
fun externalPitchScript(midi: Int, cents: Float): String {
  return """
  (function(){
    try {
      if (window.osmdIntegration && typeof window.osmdIntegration.onExternalPitch === 'function') {
        window.osmdIntegration.onExternalPitch($midi, $cents);
      } else {
        console.log('[Mic] external pitch: handler not ready');
      }
    } catch(e) { console.warn('[Mic] external pitch error:', e); }
  })();
  """
}

// ===== Piano hand audio filter control (Kotlin -> WebView) =====
// mode: "both" | "left" | "right" (any other string defaults to both)
fun setPianoHandModeScript(mode: String): String {
  val safeMode = when(mode.lowercase()) {
    "left" -> "left"
    "right" -> "right"
    else -> "both"
  }
  return """
  (function(){
    try {
      if (window.osmdIntegration && typeof window.osmdIntegration.setPianoHandMode === 'function') {
        var res = window.osmdIntegration.setPianoHandMode('$safeMode');
        if (!res.ok) { console.log('[HandMode] failed: '+ (res.reason||'unknown')); }
      } else {
        console.log('[HandMode] function not ready');
      }
    } catch(e){ console.warn('[HandMode] error:', e); }
  })();
  """.trimIndent()
}
