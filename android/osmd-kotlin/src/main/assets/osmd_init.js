// 이 파일이 실행되면서 OSMD 인스턴스가 생성됨.
// 즉 InjectionScript에서 접근하는 osmd 변수는 이 파일에서 생성되는 것임.


/* eslint-disable no-undef */
var osmd = new opensheetmusicdisplay.OpenSheetMusicDisplay('osmdContainer');
console.log("event: 'onInit', version: " + osmd.version);

/** initializes the OSMD instance within the webview. should only happen once when the webview loads.  */
osmd.autoResizeEnabled = false;
osmd.initPlaybackManager = function () {
  var timingSource = new opensheetmusicdisplay.LinearTimingSource();
  var playbackManager = new opensheetmusicdisplay.PlaybackManager(
    timingSource,
    undefined,
    new opensheetmusicdisplay.BasicAudioPlayer(),
    undefined
  );
  playbackManager.DoPlayback = true;
  timingSource.Settings = osmd.Sheet.playbackSettings;
  playbackManager.initialize(osmd.Sheet.musicPartManager);
  playbackManager.addListener(osmd.cursor);
  osmd.PlaybackManager = playbackManager;
  try { osmd.PlaybackManager.Metronome.Audible = false;
  osmd.PlaybackManager.Metronome.PreCountVolume = 0.9;} catch(_){ }

  // Defensive: sanitize listener list to avoid undefined entries causing errors on notify cycles
  try {
    var pm = osmd.PlaybackManager;
    var origAdd = pm.addListener && pm.addListener.bind(pm);
    if (origAdd) {
      pm.addListener = function(l){
        if (!l) return;
        try { origAdd(l); } catch(e){}
        try {
          var arr = pm.Listeners || pm._listeners || pm.listeners;
          if (Array.isArray(arr)) {
            for (var i = arr.length - 1; i >= 0; i--) {
              if (!arr[i]) arr.splice(i, 1);
            }
          }
        } catch(e){}
      };
    }
  } catch (e) { console.warn('[PM] listener sanitize setup failed', e); }

  // Prepare osmdIntegration namespace and define attach helper
  try {
    window.osmdIntegration = window.osmdIntegration || {};
    if (typeof window.osmdIntegration.midiToName !== 'function') {
      window.osmdIntegration.midiToName = function(midi){
        var names = ["C","C#","D","D#","E","F","F#","G","G#","A","A#","B"];
        var note = names[midi % 12]; var oct = Math.floor(midi / 12) - 1; return note + oct;
      };
    }
    if (typeof window.osmdIntegration.noteLogEnabled !== 'boolean') {
      window.osmdIntegration.noteLogEnabled = true;
    }
    if (typeof window.osmdIntegration.enableNoteLogs !== 'function') {
      window.osmdIntegration.enableNoteLogs = function(){ window.osmdIntegration.noteLogEnabled = true; console.log('[NoteLog] enabled'); };
    }
    if (typeof window.osmdIntegration.disableNoteLogs !== 'function') {
      window.osmdIntegration.disableNoteLogs = function(){ window.osmdIntegration.noteLogEnabled = false; console.log('[NoteLog] disabled'); };
    }
    window.osmdIntegration.attachNoteLogger = function(){
      if (window.osmdIntegration._loggerAttached) { console.log('[NoteLog] already attached'); return; }
      window.osmdIntegration.logPlaybackListener = {
        cursorPositionChanged: function(ts /* Fraction */, data){
          if (!window.osmdIntegration.noteLogEnabled) return;
          try{
            var notes = (osmd.cursor && osmd.cursor.NotesUnderCursor) ? osmd.cursor.NotesUnderCursor() : [];
            var midis = (notes || []).map(function(n){
              try { return (n && n.Pitch) ? (n.Pitch.getHalfTone() + 12) : 0; } catch(e) { return 0; }
            }).filter(function(m){ return m > 0; });
            var names = midis.map(window.osmdIntegration.midiToName);
            console.log('[Under Cursor]', { midis: midis, names: names, ts: ts && ts.RealValue });
          } catch(e) {}
        },
        notesPlaybackEventOccurred: function(playbackNotes){
          if (!window.osmdIntegration.noteLogEnabled) return;
          try{
            var midi = (playbackNotes || []).map(function(p){ return p && p.MidiKey; }).filter(function(k){ return typeof k === 'number' && k > 0; });
            var names = midi.map(window.osmdIntegration.midiToName);
            window.osmdIntegration.lastExpected = { midi: midi, names: names, ts: (osmd.PlaybackManager && osmd.PlaybackManager.CursorIterator && osmd.PlaybackManager.CursorIterator.CurrentEnrolledTimestamp) ? osmd.PlaybackManager.CursorIterator.CurrentEnrolledTimestamp.RealValue : undefined };
            console.log('[Expected Onset]', { midi: midi, names: names });
          } catch(e) {}
        },
        pauseOccurred: function(){}, resetOccurred: function(){}, selectionEndReached: function(){}, soundLoaded: function(){}, allSoundsLoaded: function(){}, metronomeSoundOccurred: function(){}
      };
      osmd.PlaybackManager.addListener(window.osmdIntegration.logPlaybackListener);
      window.osmdIntegration._loggerAttached = true;
      console.log('[NoteLog] listener attached (init)');
    };
    // Attach immediately now that PlaybackManager exists
    window.osmdIntegration.attachNoteLogger();
  } catch (e) { console.warn('[NoteLog] setup failed:', e); }

  // Notify Android about playback state transitions by wrapping play/pause/reset
  try {
    var pmWrap = osmd && osmd.PlaybackManager;
    if (pmWrap) {
      var _play = pmWrap.play && pmWrap.play.bind(pmWrap);
      var _pause = pmWrap.pause && pmWrap.pause.bind(pmWrap);
      var _reset = pmWrap.reset && pmWrap.reset.bind(pmWrap);
      if (_play) {
        pmWrap.play = function(){ try { if (window.AndroidOSMD && window.AndroidOSMD.playbackStateChanged) { window.AndroidOSMD.playbackStateChanged('playing'); } } catch(_){ } return _play(); };
      }
      if (_pause) {
        pmWrap.pause = function(){ try { if (window.AndroidOSMD && window.AndroidOSMD.playbackStateChanged) { window.AndroidOSMD.playbackStateChanged('paused'); } } catch(_){ } return _pause(); };
      }
      if (_reset) {
        pmWrap.reset = function(){ try { if (window.AndroidOSMD && window.AndroidOSMD.playbackStateChanged) { window.AndroidOSMD.playbackStateChanged('stopped'); } } catch(_){ } return _reset(); };
      }
    }
  } catch(e) { console.warn('[PlaybackState] wrap failed:', e); }

  // Visited measures tracker: min/max measure number reached as cursor moves (playback OR manual)
  try {
    window.osmdIntegration = window.osmdIntegration || {};
    if (!window.osmdIntegration._visitedAttached) {
      function measureNoFromIndex(idx){
        try{
          var measures = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures; if (!measures) return (idx|0)+1;
          var sm = measures[idx|0]; if (!sm) return (idx|0)+1;
          var num = (sm.MeasureNumber != null ? sm.MeasureNumber : sm.MeasureNumberXML);
          return (num != null ? num : (idx|0)+1);
        } catch(_){ return (idx|0)+1; }
      }
      function notifyAndroid(minNo, maxNo){
        try{ if (window.AndroidOSMD && window.AndroidOSMD.updateVisitedMeasures && minNo != null && maxNo != null) {
          window.AndroidOSMD.updateVisitedMeasures(minNo, maxNo);
        }} catch(__){}
      }
      var visited = window.osmdIntegration._visitedState = (window.osmdIntegration._visitedState || { min: undefined, max: undefined });
      // Allow Android to reset visited state explicitly (e.g., after initial jump)
      window.osmdIntegration.resetVisitedTo = function(no){
        try {
          var n = Number(no)||0; if (!(n>0)) return;
          visited.min = n; visited.max = n; notifyAndroid(n, n);
        } catch(e){}
      };
      window.osmdIntegration.resetVisited = function(){ try { visited.min = undefined; visited.max = undefined; } catch(e){} };
      window.osmdIntegration.updateVisitedByMeasureNo = function(no){
        try{
          if (!(no > 0)) return;
          if (visited.min == null || no < visited.min) visited.min = no;
          if (visited.max == null || no > visited.max) visited.max = no;
          notifyAndroid(visited.min, visited.max);
        } catch(e){}
      };
      window.osmdIntegration.updateVisitedFromCursor = function(){
        try{
          var it = osmd && osmd.cursor && osmd.cursor.Iterator;
          var idx = (it && typeof it.CurrentMeasureIndex === 'number') ? it.CurrentMeasureIndex : 0; // fallback to first measure
          var no = measureNoFromIndex(idx);
          window.osmdIntegration.updateVisitedByMeasureNo(no);
        } catch(e){}
      };
      // Listener for playback-driven cursor updates
      window.osmdIntegration.visitedListener = {
        cursorPositionChanged: function(ts, data){
          try{
            if (!data) return;
            var idx = data.CurrentMeasureIndex; if (typeof idx !== 'number') return;
            var no = measureNoFromIndex(idx);
            window.osmdIntegration.updateVisitedByMeasureNo(no);
          } catch(e) {}
        },
        notesPlaybackEventOccurred: function(){}, pauseOccurred: function(){}, resetOccurred: function(){}, selectionEndReached: function(){}, soundLoaded: function(){}, allSoundsLoaded: function(){}, metronomeSoundOccurred: function(){}
      };
      osmd.PlaybackManager.addListener(window.osmdIntegration.visitedListener);
      // Monkey-patch cursor methods to catch manual moves (click, mic-driven next, etc.)
      try {
        var c = osmd && osmd.cursor; if (c) {
          ['next','previous','reset','show'].forEach(function(m){
            if (typeof c[m] === 'function'){
              var orig = c[m].bind(c);
              c[m] = function(){ var r = orig.apply(c, arguments); try { window.osmdIntegration.updateVisitedFromCursor(); } catch(_){ } return r; };
            }
          });
        }
      } catch(_){ }
      window.osmdIntegration._visitedAttached = true;
    }
  } catch(e) { console.warn('[Visited] setup failed:', e); }
  // Practice Range: setup and attach enforcement
  try {
    if (!window.__practiceRange) {
      (function(){
        var state = { active:false, pickPhase:'idle', highlightVisible: false };
        function hideHighlight() {
                  try {
                    // OSMD 컨테이너에서 <svg> 요소를 찾습니다.
                    var svg = osmd.container.querySelector('svg');
                    if (svg) {
                      // <g> 그룹을 ID로 찾아 제거합니다.
                      var el = svg.querySelector('#practiceRangeHighlightGroup');
                      if (el) el.remove();
                    }
                  } catch (e) { console.warn('[PracticeRange] hideHighlight failed', e); }
                }


            /**
             * [최종 수정] 지정된 범위의 마디에 SVG 하이라이트 사각형을 그립니다.
             * (getMeasureRects 예시 로직 적용)
             * @param {number} startMeasure - 시작 마디 번호
             * @param {number} endMeasure - 끝 마디 번호
             */
            function showHighlight(startMeasure, endMeasure) {
              hideHighlight(); // 이전에 그린 하이라이트를 먼저 지웁니다.
              try {
                var svg = osmd.container.querySelector('svg');
                var graphic = osmd && osmd.graphic;
                if (!svg || !graphic || !graphic.MusicPages || !graphic.MusicPages.length) return;

                var g = document.createElementNS("http://www.w3.org/2000/svg", "g");
                g.setAttribute("id", "practiceRangeHighlightGroup");
                g.setAttribute("pointer-events", "none");

                var measuresToHighlight = new Map();

                // (1) 하이라이트할 모든 마디의 그래픽 요소를 수집합니다.
                for (var p = 0; p < graphic.MusicPages.length; p++) {
                  var page = graphic.MusicPages[p];
                  var systems = page.MusicSystems || [];
                  for (var s = 0; s < systems.length; s++) {
                    var system = systems[s];
                    var staffLines = system.StaffLines || [];
                    for (var l = 0; l < staffLines.length; l++) {
                      var staffLine = staffLines[l];
                      var measures = staffLine.Measures || [];
                      for (var m = 0; m < measures.length; m++) {
                        var gm = measures[m];
                        var sm = gm.parentSourceMeasure || gm.ParentSourceMeasure || gm.parentSourceMeasure;
                        var measureNo = (sm && (sm.MeasureNumber != null ? sm.MeasureNumber : sm.MeasureNumberXML)) || 0;

                        if (measureNo >= startMeasure && measureNo <= endMeasure) {
                          if (!measuresToHighlight.has(measureNo)) {
                            measuresToHighlight.set(measureNo, []);
                          }
                          // [!!] gm과 staffLine을 함께 저장
                          measuresToHighlight.get(measureNo).push({ gm: gm, staffLine: staffLine });
                        }
                      }
                    }
                  }
                }

                // (2) 수집된 마디별로 <rect>를 생성합니다.
                measuresToHighlight.forEach(function(gmsWithContext) { // {gm, staffLine} 객체의 배열
                  var minY = Infinity, maxY = -Infinity, x = null, width = null;

                  gmsWithContext.forEach(function(context) {
                    var gm = context.gm;
                    var staffLine = context.staffLine; // [!!] 각 gm에 맞는 staffLine
                    var measureBox = gm.PositionAndShape;
                    var staffBox = staffLine.PositionAndShape;

                    if (!measureBox || !staffBox || !measureBox.AbsolutePosition || !staffBox.AbsolutePosition) return;

                    // [!!] getMeasureRects 로직 적용
                    var left = (measureBox.AbsolutePosition.x + measureBox.BorderLeft) * 10.0;
                    var right = (measureBox.AbsolutePosition.x + measureBox.BorderRight) * 10.0;
                    var top = (staffBox.AbsolutePosition.y + staffBox.BorderTop) * 10.0;
                    var bottom = (staffBox.AbsolutePosition.y + staffBox.BorderBottom) * 10.0;
                    // [!!] 로직 끝

                    minY = Math.min(minY, top);
                    maxY = Math.max(maxY, bottom);

                    if (x === null) {
                      x = left;
                      width = right - left;
                    }
                  });

                  // 유효한 좌표가 계산된 경우에만 <rect>를 생성합니다.
                  if (x !== null && width > 0) {
                    var height = maxY - minY; // 최종 높이 계산 (예: 피아노의 상단 보표 Top ~ 하단 보표 Bottom)
                    if (height > 0) {
                        var rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
                        rect.setAttribute("x", x);
                        rect.setAttribute("y", minY);
                        rect.setAttribute("width", width);
                        rect.setAttribute("height", height);
                        rect.setAttribute("fill", "rgba(0, 200, 255, 0.18)");
                        rect.setAttribute("stroke", "rgba(0, 200, 255, 0.9)");
                        rect.setAttribute("stroke-width", "2");
                        rect.setAttribute("rx", "2");
                        g.appendChild(rect);
                    }
                  }
                });

                // (3) 완성된 <g> 그룹을 <svg>의 맨 앞에 삽입합니다.
                svg.insertBefore(g, svg.firstChild);

              } catch (e) { console.warn('[PracticeRange] showHighlight failed', e); }
            }
        /**
         * 줌 변경이나 렌더링 시 하이라이트를 다시 그립니다.
         */
        function refreshHighlight() {
            if (state.active && state.highlightVisible && state.startMeasure != null && state.endMeasure != null) {
                            showHighlight(state.startMeasure, state.endMeasure);
                        } else {
                            hideHighlight();
                        }
        }
        // Repeat state and helpers
        var repeatEnabled = false;
        var startOfPieceTs = undefined; // Fraction
        var fullEndTs = undefined; // Fraction
        var fullLoopSelectionApplied = false;

        function logRepeat(){ try { var a = Array.prototype.slice.call(arguments); a.unshift('[Repeat]'); console.log.apply(console, a); } catch(e){} }
        function computeFullEndTs(){ try { var measures = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures; if (!measures || !measures.length) return undefined; var endIdx = measures.length-1; var endStart = measures[endIdx] && (measures[endIdx].AbsoluteTimestamp.clone ? measures[endIdx].AbsoluteTimestamp.clone() : measures[endIdx].AbsoluteTimestamp); var dur = measures[endIdx] && measures[endIdx].Duration; if (!endStart || !dur || !endStart.Add) return undefined; var endTs = endStart.clone(); endTs.Add(dur); return endTs; } catch(_){ return undefined; } }
        function computeStartOfPieceTs(){ try { var measures = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures; if (!measures || !measures.length) return undefined; var start = measures[0] && measures[0].AbsoluteTimestamp; if (!start) return undefined; return start.clone ? start.clone() : start; } catch(_){ return undefined; } }
        function osmdPxToUnits(px){ var z = osmd.zoom || osmd.Zoom || 1.0; return px / (10.0*z); }
        function osmdUnitsToPx(units){ var z = osmd.zoom || osmd.Zoom || 1.0; return units * 10.0 * z; }
        function ensurePickStyles(){
          if (document.getElementById('practice-pick-styles')) return;
          var style = document.createElement('style');
          style.id = 'practice-pick-styles';
          style.textContent = '.practice-pick-highlight {\n'
            + '  position: absolute; pointer-events: none; border: 2px solid rgba(0, 200, 255, 0.9);\n'
            + '  background: rgba(0, 200, 255, 0.18); box-shadow: 0 0 0 2px rgba(0,0,0,0.05) inset, 0 0 8px rgba(0, 200, 255, 0.35);\n'
            + '  border-radius: 2px; opacity: 1; transition: opacity 450ms ease-out;\n'
            + '}\n'
            + '.practice-pick-highlight.end { border-color: rgba(255, 170, 0, 0.95); background: rgba(255, 170, 0, 0.18);\n'
            + '  box-shadow: 0 0 0 2px rgba(0,0,0,0.05) inset, 0 0 8px rgba(255, 170, 0, 0.35); }\n';
          document.head.appendChild(style);
        }

        // HUD removed for Android app; Compose shows start/end. Keep a no-op for compatibility.
        function updateHud(text){}
        function findMeasureAt(unitsX, unitsY){
          var graphic = osmd && osmd.graphic; if (!graphic || !graphic.MusicPages || !graphic.MusicPages.length) return undefined;
          for (var p=0; p<graphic.MusicPages.length; p++){
            var page = graphic.MusicPages[p]; var systems = page.MusicSystems || [];
            for (var s=0; s<systems.length; s++){
              var system = systems[s]; var staffLines = system.StaffLines || [];
              for (var l=0; l<staffLines.length; l++){
                var staffLine = staffLines[l]; var measures = staffLine.Measures || [];
                for (var m=0; m<measures.length; m++){
                  var gm = measures[m]; var bb = gm && gm.PositionAndShape; if (!bb) continue;
                  var x0 = (bb.AbsolutePosition && bb.AbsolutePosition.x) || 0; var y0 = (bb.AbsolutePosition && bb.AbsolutePosition.y) || 0;
                  var w = (bb.Size && bb.Size.width) || (bb.BorderRight - bb.BorderLeft) || 0;
                  var h = (bb.Size && bb.Size.height) || (bb.BorderBottom - bb.BorderTop) || staffLine.StaffHeight || 0;
                  var x1 = x0 + w; var y1 = y0 + h;
                  if (unitsX >= x0 && unitsX <= x1 && unitsY >= y0 && unitsY <= y1){
                    var sm = gm.parentSourceMeasure || gm.ParentSourceMeasure || gm.parentSourceMeasure;
                    var measureNo = (sm && (sm.MeasureNumber != null ? sm.MeasureNumber : sm.MeasureNumberXML)) || undefined;
                    return { gm: gm, measureNo: measureNo };
                  }
                }
              }
            }
          }
          return undefined;
        }
        function computeTimestamps(startMeasure, endMeasure){
          var measures = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures; if (!measures || !measures.length) return { ok:false, reason:'No measures' };
          var max = measures.length; var s = Math.max(1, Math.min(max, startMeasure|0)); var e = Math.max(1, Math.min(max, endMeasure|0));
          if (e < s){ var t=s; s=e; e=t; }
          var startIdx = s-1; var endIdx = e-1;
          var startTs = measures[startIdx] && (measures[startIdx].AbsoluteTimestamp.clone ? measures[startIdx].AbsoluteTimestamp.clone() : measures[startIdx].AbsoluteTimestamp);
          var endStart = measures[endIdx] && (measures[endIdx].AbsoluteTimestamp.clone ? measures[endIdx].AbsoluteTimestamp.clone() : measures[endIdx].AbsoluteTimestamp);
          var dur = measures[endIdx] && measures[endIdx].Duration; if (!startTs || !endStart || !dur || !endStart.Add) return { ok:false, reason:'Invalid ts' };
          var endTs = endStart.clone(); endTs.Add(dur);
          return { ok:true, s:s, e:e, startTs:startTs, endTs:endTs };
        }
        function attachEnforcementListener(){
                  if (!osmd || !osmd.PlaybackManager) return; if (osmd.PlaybackManager.__practiceEnforcementListenerAttached) return;
                  // cache full start/end and maybe apply full-piece selection for repeat
                  fullEndTs = computeFullEndTs(); startOfPieceTs = computeStartOfPieceTs();
                  if (repeatEnabled && !state.active && fullEndTs && startOfPieceTs){
                    try { osmd.Sheet.SelectionStart = startOfPieceTs; osmd.Sheet.SelectionEnd = fullEndTs; fullLoopSelectionApplied = true; logRepeat('Listener attach: applied full-piece selection', { start: startOfPieceTs && startOfPieceTs.RealValue, end: fullEndTs && fullEndTs.RealValue }); } catch(_){ }
                  }
                  var listener = {
                    cursorPositionChanged: function(ts){
                      try{
                        if (!ts) return;
                        var cur = ts.RealValue != null ? ts.RealValue : (ts.Numerator && ts.Denominator ? ts.Numerator/ts.Denominator : undefined);
                        if (cur == null) return;

                        // --- 연습 구간 반복 (state.active) ---
                        if (state.active && state.endTs){
                          var end = state.endTs.RealValue != null ? state.endTs.RealValue : (state.endTs.Numerator && state.endTs.Denominator ? state.endTs.Numerator/state.endTs.Denominator : undefined);
                          if (end != null && cur >= end - 1e-9){
                            osmd.PlaybackManager.pause();
                            if (state.startTs){
                              osmd.PlaybackManager.setPlaybackStart(state.startTs); // 1. setPlaybackStart (내부 reset 호출)
                              // osmd.PlaybackManager.reset(); // [!!] 2. 중복 호출 제거
                              try { osmd.cursor && osmd.cursor.show && osmd.cursor.show(); } catch(e){} // [!!] 3. 커서 즉시 이동

                              if (repeatEnabled){
                                setTimeout(function(){
                                  try { osmd.PlaybackManager.play(); logRepeat('Playback restarted from range start'); } catch(_){ }
                                }, 0);
                              }
                            }
                          }
                          return;
                        }

                        // --- 전체 곡 반복 ---
                        if (repeatEnabled && fullEndTs){
                          var endFull = fullEndTs.RealValue != null ? fullEndTs.RealValue : (fullEndTs.Numerator && fullEndTs.Denominator ? fullEndTs.Numerator/fullEndTs.Denominator : undefined);
                          logRepeat('cursorPositionChanged', { cur: cur, endFull: endFull, hasRange: state.active });
                          if (endFull != null && cur >= endFull - 1e-9){
                            logRepeat('End reached (cursor), restarting whole-piece loop');
                            osmd.PlaybackManager.pause();
                            var startTs = startOfPieceTs;
                            if (startTs){ osmd.PlaybackManager.setPlaybackStart(startTs); } else { osmd.PlaybackManager.setPlaybackStart(undefined); }
                            // osmd.PlaybackManager.reset(); // [!!] 2. 중복 호출 제거 (setPlaybackStart가 처리)
                            try { osmd.cursor && osmd.cursor.show && osmd.cursor.show(); } catch(e){} // [!!] 3. 커서 즉시 이동
                            setTimeout(function(){ try { osmd.PlaybackManager.play(); logRepeat('Playback restarted from start'); } catch(_){ } }, 0);
                          }
                        }
                      } catch(e){}
                    },
                    selectionEndReached: function(){ try{
                      // --- 연습 구간 반복 (state.active) ---
                      if (state.active){
                        logRepeat('selectionEndReached (range)');
                        osmd.PlaybackManager.pause();
                        if (state.startTs){
                          osmd.PlaybackManager.setPlaybackStart(state.startTs); // 1. setPlaybackStart (내부 reset 호출)
                          // osmd.PlaybackManager.reset(); // [!!] 2. 중복 호출 제거
                          try { osmd.cursor && osmd.cursor.show && osmd.cursor.show(); } catch(e){} // [!!] 3. 커서 즉시 이동

                          if (repeatEnabled){
                            setTimeout(function(){
                              try { osmd.PlaybackManager.play(); logRepeat('Playback restarted from range start'); } catch(_){ }
                            }, 0);
                          }
                        }
                        return;
                      }

                      // --- 전체 곡 반복 ---
                      if (repeatEnabled){
                        logRepeat('selectionEndReached (whole piece)');
                        osmd.PlaybackManager.pause();
                        var startTs = startOfPieceTs;
                        if (startTs){ osmd.PlaybackManager.setPlaybackStart(startTs); } else { osmd.PlaybackManager.setPlaybackStart(undefined); }
                        // osmd.PlaybackManager.reset(); // [!!] 2. 중복 호출 제거
                        try { osmd.cursor && osmd.cursor.show && osmd.cursor.show(); } catch(e){} // [!!] 3. 커서 즉시 이동
                        setTimeout(function(){
                          try { osmd.PlaybackManager.play(); logRepeat('Playback restarted from start (selection end)'); } catch(_){ }
                        }, 0);
                      }
                    } catch(e){} },
                    pauseOccurred: function(){}, resetOccurred: function(){}, notesPlaybackEventOccurred: function(){}, soundLoaded: function(){}, allSoundsLoaded: function(){}, metronomeSoundOccurred: function(){}
                  };
                  osmd.PlaybackManager.addListener(listener); osmd.PlaybackManager.__practiceEnforcementListenerAttached = true;
                }
        function flashMeasureHighlight(overlayEl, gm, isEndPhase){
          if (!overlayEl || !gm || !gm.PositionAndShape) return;
          var bb = gm.PositionAndShape; var x0 = (bb.AbsolutePosition && bb.AbsolutePosition.x) || 0; var y0 = (bb.AbsolutePosition && bb.AbsolutePosition.y) || 0;
          var wUnits = (bb.Size && bb.Size.width) || (bb.BorderRight - bb.BorderLeft) || 0; var hUnits = (bb.Size && bb.Size.height) || (bb.BorderBottom - bb.BorderTop) || 0;
          var hl = document.createElement('div'); hl.className = 'practice-pick-highlight' + (isEndPhase ? ' end' : '');
          hl.style.left = '' + osmdUnitsToPx(x0) + 'px'; hl.style.top = '' + osmdUnitsToPx(y0) + 'px';
          hl.style.width = '' + Math.max(0, osmdUnitsToPx(wUnits)) + 'px'; hl.style.height = '' + Math.max(0, osmdUnitsToPx(hUnits)) + 'px';
          overlayEl.appendChild(hl); setTimeout(function(){ try{ hl.style.opacity = '0'; }catch(e){} }, 60); setTimeout(function(){ try{ hl.remove(); }catch(e){} }, 550);
        }
        function setRange(start, end){var res = computeTimestamps(start, end); if (!res.ok) return;
                  osmd.Sheet.SelectionStart = res.startTs; osmd.Sheet.SelectionEnd = res.endTs; state.active = true; state.startMeasure = res.s; state.endMeasure = res.e; state.startTs = res.startTs; state.endTs = res.endTs;
                  fullLoopSelectionApplied = false; // user selection overrides synthetic full selection

                  // [!!] 수정: 하이라이트가 켜져 있을 때만 새로고침합니다.
                  if (state.highlightVisible) {
                    showHighlight(res.s, res.e);
                  } else {
                    hideHighlight();
                  }

                  if (osmd.PlaybackManager){ osmd.PlaybackManager.setPlaybackStart(state.startTs); osmd.PlaybackManager.reset(); attachEnforcementListener(); }
                  try { osmd.cursor && osmd.cursor.show && osmd.cursor.show(); } catch(e){}
                  try { updateHud(); } catch(e){}
                  try { if (window.AndroidOSMD && window.AndroidOSMD.updatePracticeRange) { window.AndroidOSMD.updatePracticeRange(state.startMeasure, state.endMeasure); } } catch(e){}
                }

 // [!!] clearRange 수정: 하이라이트 상태 초기화 및 안드로이드 알림
         function clearRange(){
           hideHighlight();
           state.highlightVisible = false; // [!!] 하이라이트 상태 초기화
           try { if (window.AndroidOSMD && window.AndroidOSMD.updateHighlightState) { window.AndroidOSMD.updateHighlightState(false); } } catch(e){} // [!!] 안드로이드에 알림

           state.active=false; state.startMeasure=undefined; state.endMeasure=undefined; state.startTs=undefined; state.endTs=undefined; if (osmd && osmd.Sheet){ osmd.Sheet.SelectionEnd = undefined; } if (osmd && osmd.PlaybackManager){ osmd.PlaybackManager.setPlaybackStart(undefined); osmd.PlaybackManager.reset(); } if (repeatEnabled && osmd){ fullEndTs = computeFullEndTs(); startOfPieceTs = computeStartOfPieceTs(); if (fullEndTs && startOfPieceTs){ try { osmd.Sheet.SelectionStart = startOfPieceTs; osmd.Sheet.SelectionEnd = fullEndTs; fullLoopSelectionApplied = true; logRepeat('clearRange reapplied full-piece selection', { start: startOfPieceTs && startOfPieceTs.RealValue, end: fullEndTs && fullEndTs.RealValue }); } catch(_){ } } } try { updateHud(); } catch(e){} try { if (window.AndroidOSMD && window.AndroidOSMD.clearPracticeRange) { window.AndroidOSMD.clearPracticeRange(); } } catch(e){}
         }
  function backToStart(){ if (!state.active || !state.startTs) return; if (osmd && osmd.PlaybackManager){ osmd.PlaybackManager.setPlaybackStart(state.startTs); osmd.PlaybackManager.reset(); } try { updateHud(); } catch(e){} try { if (window.AndroidOSMD && window.AndroidOSMD.updatePracticeRange) { window.AndroidOSMD.updatePracticeRange(state.startMeasure, state.endMeasure); } } catch(e){} }
        function startPicking(){ var container = document.getElementById('osmdContainer'); if (!container) return; if (state.picking){ stopPicking(); return; }
          state.picking = true; state.pickPhase='start'; ensurePickStyles(); updateHud(); var overlay = document.createElement('div'); overlay.id='practice-pick-overlay';
          overlay.style.position='absolute'; overlay.style.zIndex='9999'; overlay.style.cursor='crosshair'; overlay.style.background='transparent';
          var rect0 = container.getBoundingClientRect(); overlay.style.left = ''+(rect0.left+window.scrollX)+'px'; overlay.style.top = ''+(rect0.top+window.scrollY)+'px'; overlay.style.width = ''+rect0.width+'px'; overlay.style.height=''+rect0.height+'px';
          document.body.appendChild(overlay);
          var onClick = function(e){ try{ var rect = container.getBoundingClientRect(); var xUnits = osmdPxToUnits(e.clientX - rect.left); var yUnits = osmdPxToUnits(e.clientY - rect.top); var hit = findMeasureAt(xUnits, yUnits); if (!hit || !hit.measureNo) return;
              try { console.log('[PracticePick] click units ('+xUnits.toFixed(2)+', '+yUnits.toFixed(2)+'), measure='+hit.measureNo+', phase='+state.pickPhase); } catch(_){}
              try { flashMeasureHighlight(overlay, hit.gm, state.pickPhase==='end'); } catch(_){}
              if (state.pickPhase==='start'){ state.pickPhase='end'; window.__practiceRange.__startMeasure = hit.measureNo; try { updateHud(); } catch(_){ } }
              else if (state.pickPhase==='end'){ try{ e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation && e.stopImmediatePropagation(); }catch(_){}
                window.__practiceRange.__endMeasure = hit.measureNo; try{ overlay.style.pointerEvents='none'; }catch(_){}
                setTimeout(function(){ try{ stopPicking(); }catch(_){ } }, 560);
                setRange(window.__practiceRange.__startMeasure, window.__practiceRange.__endMeasure);
                setTimeout(function(){ try{ if (osmd && osmd.PlaybackManager && state.startTs){ osmd.PlaybackManager.setPlaybackStart(state.startTs); osmd.PlaybackManager.reset(); osmd.cursor && osmd.cursor.show && osmd.cursor.show(); } }catch(_){} }, 0);
              }
            }catch(_){ } };
          var onKey = function(e){ if (e.key==='Escape'){ stopPicking(); } };
          state.__pickHandlers = { onClick:onClick, onKey:onKey, overlay:overlay };
          overlay.addEventListener('click', onClick, { capture:true }); window.addEventListener('keydown', onKey);
        }
        function stopPicking(){ if (state.__pickHandlers){ try{ state.__pickHandlers.overlay && state.__pickHandlers.overlay.removeEventListener('click', state.__pickHandlers.onClick, { capture:true }); }catch(_){ }
            try{ state.__pickHandlers.overlay && state.__pickHandlers.overlay.remove(); }catch(_){ }
            try{ window.removeEventListener('keydown', state.__pickHandlers.onKey); }catch(_){ }
            state.__pickHandlers = undefined; }
          state.picking=false; state.pickPhase='idle'; }
        function setRepeatEnabled(enabled){ repeatEnabled = !!enabled; logRepeat('Toggle', { repeatEnabled: repeatEnabled, hasRange: state.active }); fullEndTs = computeFullEndTs(); startOfPieceTs = computeStartOfPieceTs(); logRepeat('Computed piece bounds', { start: startOfPieceTs && startOfPieceTs.RealValue, end: fullEndTs && fullEndTs.RealValue }); if (repeatEnabled){ if (!state.active && fullEndTs && startOfPieceTs){ try { osmd.Sheet.SelectionStart = startOfPieceTs; osmd.Sheet.SelectionEnd = fullEndTs; fullLoopSelectionApplied = true; logRepeat('Applied full-piece selection on toggle ON'); if (osmd && osmd.PlaybackManager){ osmd.PlaybackManager.setPlaybackStart(startOfPieceTs); osmd.PlaybackManager.reset(); logRepeat('Playback start set to piece start'); } } catch(_){ } } } else { if (fullLoopSelectionApplied){ try { osmd.Sheet.SelectionEnd = undefined; logRepeat('Cleared synthetic SelectionEnd on toggle OFF'); } catch(_){ } fullLoopSelectionApplied = false; } } }
        function toggleRepeat(){ setRepeatEnabled(!repeatEnabled); }
        function toggleHighlight() {
                  if (!state.active) { // 선택된 범위가 없으면 끔
                    state.highlightVisible = false;
                    hideHighlight();
                    try { if (window.AndroidOSMD && window.AndroidOSMD.updateHighlightState) { window.AndroidOSMD.updateHighlightState(false); } } catch(e){}
                    return;
                  }

                  state.highlightVisible = !state.highlightVisible; // 상태 토글

                  if (state.highlightVisible) {
                    showHighlight(state.startMeasure, state.endMeasure);
                  } else {
                    hideHighlight();
                  }
                  // 안드로이드 네이티브에 현재 상태 알림
                  try { if (window.AndroidOSMD && window.AndroidOSMD.updateHighlightState) { window.AndroidOSMD.updateHighlightState(state.highlightVisible); } } catch(e){}
                }
        window.__practiceRange = { attach: attachEnforcementListener, set: setRange, clear: clearRange, back: backToStart, startPick: startPicking, stopPick: stopPicking, state: state, setRepeatEnabled: setRepeatEnabled, toggleRepeat: toggleRepeat, refreshHighlight: refreshHighlight, toggleHighlight: toggleHighlight };
      })();
    }
    // attach now that PlaybackManager exists
    window.__practiceRange && window.__practiceRange.attach && window.__practiceRange.attach();
  } catch (e) { console.warn('[PracticePick] setup failed:', e); }

  // ===== Tempo & Metronome Bridge (for Android native UI) =====
  try {
    window.osmdIntegration = window.osmdIntegration || {};
    var pm2 = osmd.PlaybackManager;
    // readiness flag for native side
    window.osmdIntegration.ready = true;
    window.osmdIntegration.getCurrentBpm = function(){ try { return pm2.currentBPM || pm2.OriginalBpm; } catch(_){ return undefined; } };
    window.osmdIntegration.getOriginalBpm = function(){ try { return pm2.OriginalBpm; } catch(_){ return undefined; } };
    window.osmdIntegration.setBpm = function(bpm, override){
      try {
        var v = Number(bpm);
        if (!(v > 0)) return false; // PlaybackManager ignores non-positive values
        pm2.bpmChanged(v, !!override);
        return true;
      } catch(_){ return false; }
    };
    window.osmdIntegration.setMetronomeAudible = function(on){ try {
      pm2.Metronome.Audible = !!on;
      // Fallback: if turning on and volume is 0, set a sensible default so ticks are audible.
      if (pm2.Metronome.Audible && (!(pm2.Metronome.Volume > 0))) { pm2.Metronome.Volume = 0.8; }
      return true; } catch(_){ return false; } };
    window.osmdIntegration.getMetronomeAudible = function(){ try { return !!pm2.Metronome.Audible; } catch(_){ return undefined; } };
    window.osmdIntegration.getMetronomeVolume = function(){ try { return pm2.Metronome.Volume; } catch(_){ return undefined; } };
    window.osmdIntegration.getMetronomeLoaded = function(){ try { return !!pm2.metronomeLoaded; } catch(_){ return undefined; } };
    window.osmdIntegration.setPrecount = function(enabled, measures){ try { pm2.DoPreCount = !!enabled; pm2.PreCountMeasures = Math.max(1, (measures|0) || 1); pm2.reset(); return true; } catch(_){ return false; } };
    window.osmdIntegration.resetTempoToSheetStart = function(){
      try {
        var base = pm2.OriginalBpm;
        if (pm2 && pm2.musicPartManager && pm2.musicPartManager.MusicSheet){
          pm2.musicPartManager.MusicSheet.IgnoreTempoInstructions = false;
        }
        pm2.bpmChanged(base, false);
        return base;
      } catch(_){ return undefined; }
    };
    window.osmdIntegration.getTempoState = function(){
      try {
        var sheet = pm2 && pm2.musicPartManager && pm2.musicPartManager.MusicSheet;
        return {
          current: pm2 ? pm2.currentBPM : undefined,
          original: pm2 ? pm2.OriginalBpm : undefined,
          overrideActive: !!(pm2 && pm2.overrideBPM),
          ignoreTempoInstructions: !!(sheet && sheet.IgnoreTempoInstructions)
        };
      } catch(_){ return {}; }
    };
    console.log('[TempoBridge] ready');
    // Ensure cursor is visible so iterator exists, then initialize visited from cursor (with fallbacks)
    try { osmd.cursor && osmd.cursor.show && osmd.cursor.show(); } catch(e){}
    try {
      var seedVisited = function(){
        try { if (window.osmdIntegration && typeof window.osmdIntegration.updateVisitedFromCursor === 'function') { window.osmdIntegration.updateVisitedFromCursor(); } } catch(_){ }
        try {
          var v = window.osmdIntegration && window.osmdIntegration._visitedState;
          if (!v || v.min == null || v.max == null) {
            var idx = (osmd && osmd.cursor && osmd.cursor.Iterator && typeof osmd.cursor.Iterator.CurrentMeasureIndex === 'number') ? osmd.cursor.Iterator.CurrentMeasureIndex : 0;
            var measures = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures;
            var sm = measures && measures[idx||0];
            var num = sm && (sm.MeasureNumber != null ? sm.MeasureNumber : sm.MeasureNumberXML);
            var no = (num != null ? num : (idx|0)+1);
            if (window.osmdIntegration && window.osmdIntegration.updateVisitedByMeasureNo) { window.osmdIntegration.updateVisitedByMeasureNo(no); }
          }
        } catch(__){}
      };
      // schedule a couple of times to catch any late init
      setTimeout(seedVisited, 0);
      setTimeout(seedVisited, 80);
    } catch(e){}
//    // Notify Android that sheet + playback is fully ready
//    try { if (window.AndroidOSMD && window.AndroidOSMD.sheetReady) { window.AndroidOSMD.sheetReady(); } } catch(e){ console.warn('[TempoBridge] sheetReady notify failed', e); }
  } catch (e) { console.warn('[TempoBridge] setup failed:', e); }
};

// WebView mic module intentionally omitted on Android; native mic pipeline is used instead.

// ===== Volume Bridge (master & metronome) =====
(function(){
  try {
    window.osmdIntegration = window.osmdIntegration || {};
    function clamp(n, min, max){ return Math.max(min, Math.min(max, n)); }
    Object.defineProperty(window.osmdIntegration, 'setMasterVolumePercent', { value: function(percent){
      try{
        var pm2 = osmd && osmd.PlaybackManager; if (!pm2 || !pm2.audioPlayer) return false;
        var p = Number(percent); if (!(p >= 0)) return false;
        // map 0..200% -> 0.0..2.0 GainMultiplier (default 100% -> 1.0)
        var mult = clamp(p, 0, 200) / 100.0;
        pm2.audioPlayer.GainMultiplier = mult;
        return true;
      }catch(_){ return false; }
    }});
    Object.defineProperty(window.osmdIntegration, 'getMasterVolumePercent', { value: function(){
      try{
        var pm2 = osmd && osmd.PlaybackManager; if (!pm2 || !pm2.audioPlayer) return undefined;
        var mult = Number(pm2.audioPlayer.GainMultiplier || 1);
        return Math.round(mult * 100);
      }catch(_){ return undefined; }
    }});
    Object.defineProperty(window.osmdIntegration, 'setMetronomeVolumePercent', { value: function(percent){
      try{
        var pm2 = osmd && osmd.PlaybackManager; if (!pm2) return false;
        var p = clamp(Number(percent) || 0, 0, 100);
        // PlaybackManager.volumeChanged expects 0..100 for metronome with instrument -1
        pm2.volumeChanged(-1, p);
        return true;
      }catch(_){ return false; }
    }});
    Object.defineProperty(window.osmdIntegration, 'getMetronomeVolumePercent', { value: function(){
      try{
        var pm2 = osmd && osmd.PlaybackManager; if (!pm2 || !pm2.Metronome) return undefined;
        var v = Number(pm2.Metronome.Volume || 0);
        return Math.round(v * 100);
      }catch(_){ return undefined; }
    }});
    console.log('[VolumeBridge] ready');
  } catch(e) { console.warn('[VolumeBridge] setup failed', e); }
})();

// ===== Click -> Measure-start jump (no style changes) =====
// Reuse the same hit-testing as Practice Pick to map general clicks to the nearest measure,
// set SelectionStart to the measure start timestamp, reset playback iterator, and manually scroll.
(function(){
  try {
    var container = document.getElementById('osmdContainer');
    if (!container) return;

    // Local helpers so this module is independent from Practice Range internals
    function osmdPxToUnits(px){ var z = osmd.zoom || osmd.Zoom || 1.0; return px / (10.0*z); }
    function osmdUnitsToPx(units){ var z = osmd.zoom || osmd.Zoom || 1.0; return units * 10.0 * z; }
    function findMeasureAt(unitsX, unitsY){
      var graphic = osmd && osmd.graphic; if (!graphic || !graphic.MusicPages || !graphic.MusicPages.length) return undefined;
      for (var p=0; p<graphic.MusicPages.length; p++){
        var page = graphic.MusicPages[p]; var systems = page.MusicSystems || [];
        for (var s=0; s<systems.length; s++){
          var system = systems[s]; var staffLines = system.StaffLines || [];
          for (var l=0; l<staffLines.length; l++){
            var staffLine = staffLines[l]; var measures = staffLine.Measures || [];
            for (var m=0; m<measures.length; m++){
              var gm = measures[m]; var bb = gm && gm.PositionAndShape; if (!bb) continue;
              var x0 = (bb.AbsolutePosition && bb.AbsolutePosition.x) || 0; var y0 = (bb.AbsolutePosition && bb.AbsolutePosition.y) || 0;
              var w = (bb.Size && bb.Size.width) || (bb.BorderRight - bb.BorderLeft) || 0;
              var h = (bb.Size && bb.Size.height) || (bb.BorderBottom - bb.BorderTop) || staffLine.StaffHeight || 0;
              var x1 = x0 + w; var y1 = y0 + h;
              if (unitsX >= x0 && unitsX <= x1 && unitsY >= y0 && unitsY <= y1){
                var sm = gm.parentSourceMeasure || gm.ParentSourceMeasure || gm.parentSourceMeasure;
                var measureNo = (sm && (sm.MeasureNumber != null ? sm.MeasureNumber : sm.MeasureNumberXML)) || undefined;
                return { gm: gm, measureNo: measureNo };
              }
            }
          }
        }
      }
      return undefined;
    }
    function computeTimestamps(startMeasure, endMeasure){
      var measures = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures; if (!measures || !measures.length) return { ok:false, reason:'No measures' };
      var max = measures.length; var s = Math.max(1, Math.min(max, startMeasure|0)); var e = Math.max(1, Math.min(max, endMeasure|0));
      if (e < s){ var t=s; s=e; e=t; }
      var startIdx = s-1; var endIdx = e-1;
      var startTs = measures[startIdx] && (measures[startIdx].AbsoluteTimestamp.clone ? measures[startIdx].AbsoluteTimestamp.clone() : measures[startIdx].AbsoluteTimestamp);
      var endStart = measures[endIdx] && (measures[endIdx].AbsoluteTimestamp.clone ? measures[endIdx].AbsoluteTimestamp.clone() : measures[endIdx].AbsoluteTimestamp);
      var dur = measures[endIdx] && measures[endIdx].Duration; if (!startTs || !endStart || !dur || !endStart.Add) return { ok:false, reason:'Invalid ts' };
      var endTs = endStart.clone(); endTs.Add(dur);
      return { ok:true, s:s, e:e, startTs:startTs, endTs:endTs };
    }

    function manualScrollCursorIntoView(){
      try {
        var el = osmd && osmd.cursor && osmd.cursor.cursorElement;
        if (!el) return;
        var crect = container.getBoundingClientRect();
        var erect = el.getBoundingClientRect();
        var offset = (erect.top - crect.top) - (container.clientHeight * 0.35);
        // Clamp to range
        var target = Math.max(0, container.scrollTop + offset);
        container.scrollTop = target;
      } catch(_){}
    }

    function handleClickToMeasure(e){
      try {
        // Ignore during practice picking
        if (window.__practiceRange && window.__practiceRange.state && window.__practiceRange.state.picking) return;
        var rect = container.getBoundingClientRect();
        var xUnits = osmdPxToUnits(e.clientX - rect.left);
        var yUnits = osmdPxToUnits(e.clientY - rect.top);
        var hit = findMeasureAt(xUnits, yUnits);
        if (!hit || !hit.measureNo) return;
  try { if (window.osmdIntegration && window.osmdIntegration.updateVisitedByMeasureNo) { window.osmdIntegration.updateVisitedByMeasureNo(hit.measureNo); } } catch(_){ }
        // Flash highlight of clicked measure for user feedback (reuse style concept from practice pick)
        try {
          var gm = hit.gm; var bb = gm && gm.PositionAndShape; if (bb) {
            var x0u = (bb.AbsolutePosition && bb.AbsolutePosition.x) || 0; var y0u = (bb.AbsolutePosition && bb.AbsolutePosition.y) || 0;
            var wUnits = (bb.Size && bb.Size.width) || (bb.BorderRight - bb.BorderLeft) || 0; var hUnits = (bb.Size && bb.Size.height) || (bb.BorderBottom - bb.BorderTop) || 0;
            var xPx = osmdUnitsToPx(x0u); var yPx = osmdUnitsToPx(y0u); var wPx = Math.max(0, osmdUnitsToPx(wUnits)); var hPx = Math.max(0, osmdUnitsToPx(hUnits));
            // Ensure relative positioning context
            if (getComputedStyle(container).position === 'static') { try { container.style.position='relative'; } catch(_){ } }
            // Remove previous raw highlight
            var prev = container.querySelector('.raw-click-measure-highlight'); if (prev) { try { prev.remove(); } catch(_){ } }
            var hl = document.createElement('div'); hl.className='raw-click-measure-highlight';
            hl.style.position='absolute'; hl.style.left = ''+xPx+'px'; hl.style.top=''+yPx+'px'; hl.style.width=''+wPx+'px'; hl.style.height=''+hPx+'px';
            hl.style.pointerEvents='none'; hl.style.border='2px solid rgba(0,160,255,0.9)'; hl.style.background='rgba(0,160,255,0.18)';
            hl.style.boxShadow='0 0 0 2px rgba(0,0,0,0.05) inset, 0 0 8px rgba(0,160,255,0.35)'; hl.style.borderRadius='2px'; hl.style.opacity='1'; hl.style.transition='opacity 450ms ease-out';
            container.appendChild(hl); setTimeout(function(){ try { hl.style.opacity='0'; } catch(_){ } }, 60); setTimeout(function(){ try { hl.remove(); } catch(_){ } }, 550);
          }
        } catch(_){ }
        var ts = computeTimestamps(hit.measureNo, hit.measureNo);
        if (!ts || !ts.ok) return;
        // Apply selection & iterator
        try { osmd.Sheet.SelectionStart = ts.startTs; osmd.Sheet.SelectionEnd = undefined; } catch(_){ }
        if (osmd && osmd.PlaybackManager) {
          try { osmd.PlaybackManager.setPlaybackStart(ts.startTs); osmd.PlaybackManager.reset(); } catch(_){ }
        }
        try { osmd.cursor && osmd.cursor.show && osmd.cursor.show(); } catch(_){ }
        manualScrollCursorIntoView();
        // Block OSMD's internal click handlers so they don't overwrite SelectionStart to a note timestamp
        try { e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation && e.stopImmediatePropagation(); } catch(_){ }
      } catch(err){ console.warn('[Click->Measure] error', err); }
    }

    // Use capture so our handler runs before any internal SVG listeners
    container.addEventListener('click', handleClickToMeasure, { capture: true });
    // Block internal OSMD pointer/mouse handlers from changing selection (but don't prevent default so click still fires)
    function blockRawPointer(e){
      try {
        if (window.__practiceRange && window.__practiceRange.state && window.__practiceRange.state.picking) return;
        // Only block primary button / single-touch; allow gestures like pinch-zoom
        if (e.type === 'pointerdown' || e.type === 'pointerup' || e.type === 'mousedown' || e.type === 'mouseup'){
          if (typeof e.button === 'number' && e.button !== 0) return;
        }
        if (e.touches && e.touches.length > 1) return;
        e.stopPropagation();
        if (e.stopImmediatePropagation) e.stopImmediatePropagation();
      } catch(_){}
    }
    container.addEventListener('pointerdown', blockRawPointer, { capture: true });
    container.addEventListener('pointerup', blockRawPointer, { capture: true });
    container.addEventListener('mousedown', blockRawPointer, { capture: true });
    container.addEventListener('mouseup', blockRawPointer, { capture: true });
    // Touch support on mobile
    container.addEventListener('touchstart', blockRawPointer, { capture: true, passive: true });
    container.addEventListener('touchend', blockRawPointer, { capture: true, passive: true });
  } catch (e) { console.warn('[Click->Measure] setup failed', e); }
})();

// ===== Piano hand audio filter (keep both staves visible, filter audio only) =====
(function(){
  if (!window.osmdIntegration) window.osmdIntegration = {};
  if (window.osmdIntegration.setPianoHandMode) return;

  function findTwoStaffInstrument(){
    try {
      var sheet = osmd && osmd.Sheet; if (!sheet || !sheet.Instruments || !sheet.Instruments.length) return undefined;
      var cand = undefined;
      for (var i=0;i<sheet.Instruments.length;i++){
        var inst = sheet.Instruments[i];
        var name = '' + (inst && (inst.Name || (inst.NameLabel && inst.NameLabel.text) || inst.NameLabelText || ''));
        var twoStaves = inst && inst.Staves && inst.Staves.length >= 2;
        if (twoStaves && name.toLowerCase().indexOf('piano') >= 0) { cand = inst; break; }
        if (!cand && twoStaves) cand = inst;
      }
      if (!cand || !cand.Staves || cand.Staves.length < 2) return undefined;
      return { inst: cand, upper: cand.Staves[0], lower: cand.Staves[1] };
    } catch(e){ return undefined; }
  }

  window.osmdIntegration.setPianoHandMode = function(mode){
    try {
      var found = findTwoStaffInstrument();
      if (!found) { return { ok:false, reason:'no two-staff instrument' }; }
      var upper = found.upper, lower = found.lower;
      // Always keep visual visible
      try { if (upper && upper.Visible === false) upper.Visible = true; } catch(_){ }
      try { if (lower && lower.Visible === false) lower.Visible = true; } catch(_){ }
      function setAudible(staff, val){ try { if (staff && ("audible" in staff)) staff.audible = !!val; } catch(_){ } }
      var m = (''+mode).toLowerCase();
      if (m === 'right') { setAudible(upper, true); setAudible(lower, false); }
      else if (m === 'left') { setAudible(upper, false); setAudible(lower, true); }
      else { setAudible(upper, true); setAudible(lower, true); m = 'both'; }
      window.osmdIntegration._lastHandMode = m;
      return { ok:true, mode:m };
    } catch(e){ return { ok:false, reason: (e && e.message) || 'error' }; }
  };
})();

// External pitch input from native (Android) pipeline.
// Call window.osmdIntegration.onExternalPitch(midi, cents)
// to reuse the same matching & cursor-advance behavior without using WebView getUserMedia.
(function(){
  if (!window.osmdIntegration) window.osmdIntegration = {};
  if (window.osmdIntegration.onExternalPitch) return;

  var pitchToleranceCents = 35; // 기본값 35

  // Mic matching config (cooldown only)
  var micConfig = {
    advanceCooldownMs: 100,
    _lastAdvanceTs: 0
  };

  // [!!] (추가) 2. 음정 허용치 설정 브릿지
    Object.defineProperty(window.osmdIntegration, 'setPitchToleranceCents', { value: function(cents){
        try {
          var v = Number(cents);
          if (!(v > 0)) v = 35;
          pitchToleranceCents = v;
          console.log('[PitchTolerance] set to ' + pitchToleranceCents + ' cents');
          return true;
        } catch(_){ return false; }
    }});

  Object.defineProperty(window.osmdIntegration, 'setMicAdvanceCooldownMs', { value: function(ms){ try { var v = Math.max(0, Math.min(2000, Number(ms)||0)); micConfig.advanceCooldownMs = v; return true; } catch(_){ return false; } }});
  function expectedMidis(){
    try {
      var notes = (osmd.cursor && osmd.cursor.NotesUnderCursor) ? osmd.cursor.NotesUnderCursor() : [];
      var midis = (notes || []).map(function(n){
        try { return (n && n.Pitch) ? (n.Pitch.getHalfTone() + 12) : 0; } catch(e) { return 0; }
      }).filter(function(m){ return m > 0; });
      return midis;
    } catch(e) { return []; }
  }
  function syncPlaybackToCursor(){
    try {
      var iter = osmd && osmd.cursor && osmd.cursor.Iterator;
      var ts = iter && (iter.CurrentEnrolledTimestamp || iter.currentTimeStamp);
      if (ts) {
        if (osmd.PlaybackManager && osmd.PlaybackManager.setPlaybackStart) osmd.PlaybackManager.setPlaybackStart(ts);
        else if (osmd.Sheet) osmd.Sheet.SelectionStart = ts;
      }
    } catch(e){}
  }
  function isCursorAtSheetEnd(){
    try {
      var it = osmd && osmd.cursor && osmd.cursor.Iterator;
      var measures = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures;
      if (!it || !measures) return false;
      return it.CurrentMeasureIndex >= measures.length;
    } catch(e){ return false; }
  }
  function currentMeasureNumber(){
    try {
      var it = osmd && osmd.cursor && osmd.cursor.Iterator;
      var idx = (it && typeof it.CurrentMeasureIndex === 'number') ? it.CurrentMeasureIndex : 0;
      var measures = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures;
      var sm = measures && measures[idx||0];
      var num = sm && (sm.MeasureNumber != null ? sm.MeasureNumber : sm.MeasureNumberXML);
      return (num != null ? num : (idx|0)+1);
    } catch(_){ return 1; }
  }
  function advanceCursorAndNotify(sourceTag, midi, cents){
    if (isCursorAtSheetEnd()) {
      return false;
    }
    var measureNo = currentMeasureNumber();
    try {
      if (window.osmdIntegration && typeof window.osmdIntegration.updateVisitedFromCursor === 'function') {
        window.osmdIntegration.updateVisitedFromCursor();
      }
    } catch(_){ }
    osmd.cursor && osmd.cursor.next && osmd.cursor.next();
    osmd.cursor && osmd.cursor.show && osmd.cursor.show();
    syncPlaybackToCursor();
    try {
      if (window.AndroidOSMD && typeof window.AndroidOSMD.pitchAccepted === 'function') {
        var midiVal = (typeof midi === 'number' && midi > 0) ? midi : 0;
        var centsVal = (typeof cents === 'number') ? cents : 0;
        window.AndroidOSMD.pitchAccepted(midiVal, centsVal, measureNo);
      }
    } catch(_){ }
    try { console.log('[Mic] advance via ' + (sourceTag || 'unknown'), { measure: measureNo }); } catch(_){ }
    return true;
  }
  window.osmdIntegration.onExternalPitch = function(midi, cents){
      // [수정] 가장 바깥쪽 try 블록
      try {

        // --- [블록 0: 신규 방어 코드] ---
        if (isCursorAtSheetEnd()) {
          console.log('[Mic] Cursor already past end. Ignoring pitch.');
          return;
        }
        // --- [블록 0 끝] ---

        var now = Date.now();
        if (now - micConfig._lastAdvanceTs < micConfig.advanceCooldownMs) { return; }
        var exp = expectedMidis();
        var ok = false;
        if (!exp || !exp.length) {
          ok = (typeof midi === 'number' && midi > 0);
          if (ok) { try { console.log('[Mic] rest-advance', { midi: midi, cents: cents }); } catch(_){ } }
        } else {
          ok = exp.some(function(m){ return m === midi; }) && Math.abs(cents) <= pitchToleranceCents;
          try { if (ok) console.log('[Mic] match', { midi: midi, cents: cents, expected: exp }); } catch(_){ }
        }
        if (!ok) return;
        micConfig._lastAdvanceTs = now;
        advanceCursorAndNotify('mic', midi, cents);

      // [수정] 가장 바깥쪽 try에 대한 catch 블록
      } catch(e) {
        console.warn('[Mic] onExternalPitch global error', e);
      }
    };

  var restAutoConfig = {
    enabled: false,
    timer: null,
    restSince: null,
    requiredHoldMs: 50,
    pollMs: 60
  };

  function scheduleRestAutoTick(){
    if (!restAutoConfig.enabled) { return; }
    restAutoConfig.timer = setTimeout(function(){
      try {
        if (!restAutoConfig.enabled) { return; }
        if (isCursorAtSheetEnd()) { restAutoConfig.restSince = null; return; }
        var exp = expectedMidis();
        var isRest = !exp || exp.length === 0;
        if (isRest) {
          var now = Date.now();
          if (!restAutoConfig.restSince) { restAutoConfig.restSince = now; }
          var held = now - restAutoConfig.restSince;
          if (held >= restAutoConfig.requiredHoldMs && (now - micConfig._lastAdvanceTs) >= micConfig.advanceCooldownMs) {
            micConfig._lastAdvanceTs = now;
            restAutoConfig.restSince = null;
            advanceCursorAndNotify('rest-auto', 0, 0);
          }
        } else {
          restAutoConfig.restSince = null;
        }
      } finally {
        scheduleRestAutoTick();
      }
    }, restAutoConfig.pollMs);
  }

  Object.defineProperty(window.osmdIntegration, 'setRestAutoAdvanceEnabled', { value: function(enabled){
    var flag = !!enabled;
    if (restAutoConfig.enabled === flag) return true;
    restAutoConfig.enabled = flag;
    if (!flag) {
      if (restAutoConfig.timer) { clearTimeout(restAutoConfig.timer); }
      restAutoConfig.timer = null;
      restAutoConfig.restSince = null;
      return true;
    }
    if (!restAutoConfig.timer) {
      scheduleRestAutoTick();
    }
    return true;
  }});
})();

// ===== Lightweight getters and navigation helpers (Android bridge) =====
(function(){
  try {
    window.osmdIntegration = window.osmdIntegration || {};
    function manualScrollCursorIntoView(){
          try {
            var container = document.getElementById('osmdContainer'); // [!!] 컨테이너 ID 확인
            var el = osmd && osmd.cursor && osmd.cursor.cursorElement;
            if (!el || !container) return; // [!!] 컨테이너 null 체크 추가
            var crect = container.getBoundingClientRect();
            var erect = el.getBoundingClientRect();
            var offset = (erect.top - crect.top) - (container.clientHeight * 0.35);
            // Clamp to range
            var target = Math.max(0, container.scrollTop + offset);
            container.scrollTop = target;
          } catch(_){}
        }
    Object.defineProperty(window.osmdIntegration, 'getTotalMeasures', { value: function(){ try { var ms = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures; return (ms && ms.length) ? ms.length : 0; } catch(_){ return 0; } }});
    Object.defineProperty(window.osmdIntegration, 'getCurrentMeasureNo', { value: function(){ try { var it = osmd && osmd.cursor && osmd.cursor.Iterator; var idx = (it && typeof it.CurrentMeasureIndex === 'number') ? it.CurrentMeasureIndex : 0; var ms = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures; var sm = ms && ms[idx||0]; var num = sm && (sm.MeasureNumber != null ? sm.MeasureNumber : sm.MeasureNumberXML); return (num != null ? num : (idx|0)+1); } catch(_){ return 1; } }});
    Object.defineProperty(window.osmdIntegration, 'getPracticeRange', { value: function(){ try { var ms = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures; if (!ms || !ms.length) return null; var st = osmd && osmd.Sheet && osmd.Sheet.SelectionStart; var en = osmd && osmd.Sheet && osmd.Sheet.SelectionEnd; if (!st || !en) return null; var findIdx = function(ts){ for (var i=0;i<ms.length;i++){ var m = ms[i]; if (m && m.AbsoluteTimestamp && ts && m.AbsoluteTimestamp.RealValue === ts.RealValue) return i; } return null; }; var sIdx = findIdx(st) || 0; var eIdx = findIdx(en) || sIdx; var sNo = (ms[sIdx] && (ms[sIdx].MeasureNumber != null ? ms[sIdx].MeasureNumber : ms[sIdx].MeasureNumberXML)) || (sIdx+1); var eNo = (ms[eIdx] && (ms[eIdx].MeasureNumber != null ? ms[eIdx].MeasureNumber : ms[eIdx].MeasureNumberXML)) || (eIdx+1); return { start: sNo, end: eNo }; } catch(_){ return null; } }});
    Object.defineProperty(window.osmdIntegration, 'goToMeasure', { value: function(no){ try {
         console.log('[Verify] 6. goToMeasure(' + no + ') EXECUTED');
         var n = Number(no)||1; var measures = osmd && osmd.Sheet && osmd.Sheet.SourceMeasures; if (!measures || !measures.length) return false; n = Math.max(1, Math.min(measures.length, n));

         // [!!] (수정) 목표 인덱스(idx)를 함수 스코프에서 접근 가능하도록 var로 변경
         var idx = n-1;

         var startTs = measures[idx] && (measures[idx].AbsoluteTimestamp.clone ? measures[idx].AbsoluteTimestamp.clone() : measures[idx].AbsoluteTimestamp); if (!startTs) return false;

         osmd.Sheet.SelectionStart = startTs;
         osmd.Sheet.SelectionEnd = undefined;

         // 1. PlaybackManager를 리셋합니다. (이때 Iterator가 업데이트됨)
         if (osmd && osmd.PlaybackManager) {
            osmd.PlaybackManager.setPlaybackStart(startTs);
            osmd.PlaybackManager.reset();
         }

         // 2. 커서 그리기를 '요청'합니다. (DOM 업데이트는 비동기)
         osmd.cursor && osmd.cursor.show && osmd.cursor.show();

         // [!!] 3. (수정) "데이터"와 "뷰"가 모두 일치하는지 확인하는 폴링
         var maxAttempts = 40; // 최대 2초
         var attempt = 0;
         function pollForDataAndViewSync() {
            attempt++;

            // --- 1. 데이터(Iterator) 확인 ---
            var iteratorIndex = -1;
            try { iteratorIndex = (osmd && osmd.cursor && osmd.cursor.Iterator) ? osmd.cursor.Iterator.CurrentMeasureIndex : -1; } catch(e) {}

            // [!!] 데이터가 아직 목표(idx)에 도달하지 못했으면 재시도
            if (iteratorIndex !== idx) {
                console.log('[Verify] Iterator not yet at target index (is ' + iteratorIndex + ', want ' + idx + '). Retrying... (attempt ' + attempt + ')');
                if (attempt < maxAttempts) setTimeout(pollForDataAndViewSync, 50);
                return;
            }

            // --- 2. 뷰(DOM) 확인 (데이터가 일치할 때만) ---
            var cursorEl = osmd && osmd.cursor && osmd.cursor.cursorElement;
            if (cursorEl) {
                var rect = cursorEl.getBoundingClientRect();
                // [!!] 뷰의 레이아웃이 유효한지 확인
                if (rect && rect.height > 0) {
                    // [성공] 데이터와 뷰가 모두 준비됨.
                    console.log('[Verify] Iterator AND Layout valid on attempt ' + attempt + ', scrolling now.');
                    manualScrollCursorIntoView();
                } else {
                    // [재시도] 데이터는 맞으나 뷰가 아직 안 그려짐 (height=0)
                    console.log('[Verify] Iterator is at target, but layout invalid (height=0). Retrying... (attempt ' + attempt + ')');
                    if (attempt < maxAttempts) setTimeout(pollForDataAndViewSync, 50);
                }
            } else {
                 // [재시도] 데이터는 맞으나 뷰(DOM)가 없음
                 console.log('[Verify] Iterator is at target, but cursor element not found. Retrying... (attempt ' + attempt + ')');
                 if (attempt < maxAttempts) setTimeout(pollForDataAndViewSync, 50);
            }
         }

         // [!!] 4. (수정) 폴링 시작 지연 시간 (100ms) 제거.
         // Iterator가 업데이트되는 즉시 폴링을 시작해도 됩니다.
         pollForDataAndViewSync();

         // 5. 방문 기록 업데이트 (이건 스크롤과 무관하게 바로 실행)
         try { if (window.osmdIntegration && window.osmdIntegration.updateVisitedByMeasureNo) { window.osmdIntegration.updateVisitedByMeasureNo(n); } } catch(_){ }

         return true;
    } catch(e){ return false; } }});
    console.log('[AndroidBridge] getters/navigation ready');
  } catch(e) { console.warn('[AndroidBridge] setup failed', e); }
})();
