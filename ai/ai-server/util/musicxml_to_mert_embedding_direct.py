# musicxml_to_mert_embedding_direct.py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys, os, json, tempfile, shutil, subprocess
from pathlib import Path
import numpy as np
import torch, torchaudio
import music21
from transformers import AutoProcessor, AutoModel

# 경고 억제
import warnings
warnings.filterwarnings('ignore')
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

# ===== 여기에 파일 경로 지정! =====
INPUT_FILE = "Brahms_violin_concerto.musicxml"
OUTPUT_JSON = "Brahms_violin_concerto.json"
# ===================================

MODEL_NAME = "m-a-p/MERT-v1-330M"
_model = None
_processor = None

def _log(msg):
    print(str(msg), file=sys.stderr, flush=True)

def _exists(p):
    return p and os.path.exists(p)

def _rm_safe(p):
    try:
        if p and os.path.exists(p):
            os.remove(p)
    except Exception:
        pass

# ===== 반복 기호 처리 =====
def sanitize_repeats_and_jumps(score):
    try:
        remove_classes = ['Repeat', 'Volta', 'RepeatBracket', 'RepeatExpression']
        for class_name in remove_classes:
            try:
                for el in list(score.recurse().getElementsByClass(class_name)):
                    try:
                        if hasattr(el, 'activeSite') and el.activeSite:
                            el.activeSite.remove(el)
                    except:
                        pass
            except:
                pass
        
        for t in list(score.recurse().getElementsByClass('TextExpression')):
            try:
                txt = (t.content or '').lower()
                if any(k in txt for k in ['segno', 'coda', 'd.c.', 'd.s.', 'da capo', 'dal segno', 'fine']):
                    if hasattr(t, 'activeSite') and t.activeSite:
                        t.activeSite.remove(t)
            except:
                pass
    except Exception as e:
        _log(f"반복 기호 제거 경고: {e}")
    return score

def safe_expand_repeats(score):
    try:
        expanded = score.expandRepeats()
        return expanded
    except Exception as e:
        _log(f"반복 확장 실패, 원본 사용: {e}")
        return score

# ===== MusicXML → MIDI =====
def musicxml_to_midi(xml_path):
    try:
        _log(f"MusicXML 파싱: {xml_path}")
        score = music21.converter.parse(xml_path)
        score = sanitize_repeats_and_jumps(score)
        score = safe_expand_repeats(score)

        temp_dir = tempfile.gettempdir()
        midi_path = os.path.join(temp_dir, f"temp_{os.getpid()}.mid")
        
        _log(f"MIDI 변환: {midi_path}")
        score.write('midi', fp=midi_path)
        
        if not os.path.exists(midi_path):
            raise Exception("MIDI 파일 생성 실패")
        
        return midi_path, score
    except Exception as e:
        raise Exception(f"MusicXML→MIDI 실패: {e}")

# ===== Python으로 MIDI → WAV =====
def midi_to_wav_python(midi_path, wav_path, sr=16000):
    """Python으로 MIDI → WAV"""
    try:
        from mido import MidiFile
        import soundfile as sf
        
        _log("[Python] MIDI → WAV 변환 중...")
        
        midi = MidiFile(midi_path)
        duration = midi.length + 1
        samples = int(duration * sr)
        audio = np.zeros(samples, dtype=np.float32)
        
        time = 0
        active_notes = {}
        
        for msg in midi:
            time += msg.time
            
            if msg.type == 'note_on' and msg.velocity > 0:
                freq = 440 * (2 ** ((msg.note - 69) / 12))
                active_notes[msg.note] = {
                    'freq': freq,
                    'start': int(time * sr),
                    'velocity': msg.velocity / 127.0
                }
                
            elif msg.type == 'note_off' or (msg.type == 'note_on' and msg.velocity == 0):
                if msg.note in active_notes:
                    note = active_notes[msg.note]
                    start = note['start']
                    end = min(int(time * sr), samples)
                    length = end - start
                    
                    if length > 0:
                        t = np.arange(length) / sr
                        sine = np.sin(2 * np.pi * note['freq'] * t) * note['velocity']
                        
                        attack = min(int(0.01 * sr), length // 4)
                        if attack > 0:
                            sine[:attack] *= np.linspace(0, 1, attack)
                        
                        release = min(int(0.05 * sr), length // 4)
                        if release > 0:
                            sine[-release:] *= np.linspace(1, 0, release)
                        
                        audio[start:end] += sine
                    
                    del active_notes[msg.note]
        
        max_val = np.abs(audio).max()
        if max_val > 0:
            audio = audio / max_val * 0.8
        
        sf.write(wav_path, audio, sr)
        _log(f"[Python] WAV 생성 완료: {wav_path}")
        return wav_path
        
    except ImportError:
        raise ImportError("pip install mido soundfile 실행 필요")
    except Exception as e:
        raise Exception(f"Python MIDI→WAV 실패: {e}")

# ===== MuseScore 찾기 =====
def find_musescore():
    candidates_path = ["musescore4", "musescore3", "MuseScore4", "MuseScore3", "mscore"]
    for candidate in candidates_path:
        path = shutil.which(candidate)
        if path:
            _log(f"MuseScore 발견: {path}")
            return path
    
    direct_paths = [
        r"C:\Program Files\MuseScore 4\bin\MuseScore4.exe",
        r"C:\Program Files\MuseScore 3\bin\MuseScore3.exe",
        r"C:\Program Files (x86)\MuseScore 4\bin\MuseScore4.exe",
        r"C:\Program Files (x86)\MuseScore 3\bin\MuseScore3.exe",
    ]
    
    for path in direct_paths:
        if os.path.exists(path):
            _log(f"MuseScore 발견: {path}")
            return path
    
    return None

# ===== MIDI → WAV =====
def midi_to_wav(midi_path, sr=24000):
    wav_path = os.path.splitext(midi_path)[0] + ".wav"
    
    # MuseScore 시도
    mscore = find_musescore()
    if mscore:
        try:
            _log(f"[MuseScore] 시도: {mscore}")
            cmd = [mscore, "-o", wav_path, midi_path]
            subprocess.run(
                cmd, 
                check=True, 
                capture_output=True, 
                text=True, 
                timeout=60,
                creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == 'win32' else 0
            )
            
            if _exists(wav_path) and os.path.getsize(wav_path) > 0:
                _log("[MuseScore] 성공!")
                return wav_path
        except Exception as e:
            _log(f"[MuseScore] 실패: {e}")
    
    # Python 백업
    try:
        return midi_to_wav_python(midi_path, wav_path, sr)
    except Exception as e:
        _log(f"[Python] 실패: {e}")
    
    raise RuntimeError("MIDI→WAV 변환 실패!\npip install mido soundfile")

# ===== MERT 모델 =====
def load_model():
    global _model, _processor
    if _model is not None:
        return
    
    _log(f"MERT 모델 로딩... ({MODEL_NAME})")
    _processor = AutoProcessor.from_pretrained(MODEL_NAME, trust_remote_code=True)
    _model = AutoModel.from_pretrained(MODEL_NAME, trust_remote_code=True)
    _model.eval()
    
    if torch.cuda.is_available():
        _model = _model.cuda()
        _log("✓ GPU 사용")
    else:
        _log("✓ CPU 사용")

def wav_to_embedding_768(wav_path, target_sr=24000):
    load_model()
    
    _log(f"오디오 로딩: {wav_path}")
    waveform, sr = torchaudio.load(wav_path)
    
    if waveform.shape[0] > 1:
        waveform = torch.mean(waveform, dim=0, keepdim=True)
    
    if sr != target_sr:
        _log(f"리샘플링: {sr}Hz → {target_sr}Hz")
        waveform = torchaudio.transforms.Resample(sr, target_sr)(waveform)
    
    win = target_sr * 20
    hop = target_sr * 10
    T = waveform.shape[1]
    
    chunks = []
    if T <= win:
        chunks = [waveform]
    else:
        for start in range(0, max(T - win + 1, 1), hop):
            chunks.append(waveform[:, start:start + win])
        if (T - win) % hop != 0:
            chunks.append(waveform[:, -win:])
    
    _log(f"청크 개수: {len(chunks)}")
    
    embs = []
    for i, chunk in enumerate(chunks):
        inputs = _processor(chunk.squeeze().numpy(), sampling_rate=target_sr, return_tensors="pt")
        
        if torch.cuda.is_available():
            inputs = {k: v.cuda() for k, v in inputs.items()}
        
        with torch.no_grad():
            outputs = _model(**inputs, output_hidden_states=True)
            hidden = outputs.last_hidden_state
            emb = torch.mean(hidden, dim=1).squeeze().cpu().numpy()
            embs.append(emb)
    
    embedding = np.mean(np.stack(embs, axis=0), axis=0) if len(embs) > 1 else embs[0]
    
    norm = np.linalg.norm(embedding)
    if norm > 1e-8:
        embedding = embedding / norm
    
    _log("✓ 임베딩 추출 완료")
    return embedding.astype(np.float32)

# ===== 메타데이터 =====
def extract_basic_features(score):
    feats = {}
    
    try:
        key = score.analyze('key')
        feats['key_signature'] = str(key)
        feats['key_mode'] = key.mode
    except:
        feats['key_signature'] = 'C major'
        feats['key_mode'] = 'major'
    
    try:
        ts_list = score.recurse().getElementsByClass(music21.meter.TimeSignature)
        if ts_list:
            ts = ts_list[0]
            feats['time_signature'] = f"{ts.numerator}/{ts.denominator}"
        else:
            feats['time_signature'] = "4/4"
    except:
        feats['time_signature'] = "4/4"
    
    try:
        tempo_marks = score.recurse().getElementsByClass(music21.tempo.MetronomeMark)
        if tempo_marks and hasattr(tempo_marks[0], 'number'):
            feats['tempo'] = int(tempo_marks[0].number)
        else:
            feats['tempo'] = 120
    except:
        feats['tempo'] = 120
    
    try:
        notes = list(score.recurse().notes)
        pitches = [n.pitch.midi for n in notes if hasattr(n, 'isNote') and n.isNote]
        if pitches:
            feats['pitch_range'] = max(pitches) - min(pitches)
        else:
            feats['pitch_range'] = 24
    except:
        feats['pitch_range'] = 24
    
    try:
        measures = list(score.recurse().getElementsByClass(music21.stream.Measure))
        feats['num_measures'] = len(measures)
    except:
        feats['num_measures'] = 0
    
    try:
        feats['num_parts'] = len(score.parts) if hasattr(score, 'parts') else 1
    except:
        feats['num_parts'] = 1
    
    return feats

# ===== 메인 =====
def musicxml_to_mert_embedding(input_path):
    p = Path(input_path)
    suffix = p.suffix.lower()

    midi_path = None
    wav_path = None
    score = None
    cleanup_midi = False
    cleanup_wav = False

    try:
        if suffix in [".xml", ".musicxml", ".mxl"]:
            _log(f"MusicXML 감지: {p.name}")
            midi_path, score = musicxml_to_midi(str(p))
            cleanup_midi = True
            
        elif suffix in [".mid", ".midi"]:
            _log(f"MIDI 감지: {p.name}")
            midi_path = str(p)
            score = music21.converter.parse(str(p))
            
        elif suffix in [".wav", ".mp3"]:
            _log(f"오디오 감지: {p.name}")
            wav_path = str(p)
            score = None
        else:
            raise ValueError(f"지원 안함: {suffix}")

        if not wav_path:
            wav_path = midi_to_wav(midi_path, sr=24000)
            cleanup_wav = True

        embedding = wav_to_embedding_768(wav_path, target_sr=24000)
        
        if score:
            features = extract_basic_features(score)
        elif midi_path:
            try:
                score = music21.converter.parse(midi_path)
                features = extract_basic_features(score)
            except:
                features = {}
        else:
            features = {}

        return embedding, features

    finally:
        if cleanup_midi and midi_path:
            _rm_safe(midi_path)
        if cleanup_wav and wav_path:
            _rm_safe(wav_path)

def main():
    # 파일 존재 확인
    if not os.path.exists(INPUT_FILE):
        error = {
            "success": False,
            "error": f"파일을 찾을 수 없습니다: {INPUT_FILE}"
        }
        print(json.dumps(error, ensure_ascii=False, indent=2))
        return
    
    try:
        _log("=" * 60)
        _log(f"입력 파일: {INPUT_FILE}")
        _log(f"출력 파일: {OUTPUT_JSON}")
        _log("=" * 60)
        
        # 임베딩 생성
        embedding, features = musicxml_to_mert_embedding(INPUT_FILE)
        
        # 결과 JSON
        result = {
            "success": True,
            "input_file": INPUT_FILE,
            "embedding_method": MODEL_NAME,
            "embedding_dim": int(embedding.shape[0]),
            "embedding": embedding.tolist(),
            "features": features
        }
        
        # JSON 파일로 저장
        with open(OUTPUT_JSON, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        
        _log("=" * 60)
        _log(f"✓ 완료! JSON 저장: {OUTPUT_JSON}")
        _log("=" * 60)
        
    except Exception as e:
        error = {
            "success": False,
            "error": str(e),
            "error_type": type(e).__name__
        }
        
        # 에러도 JSON 파일로 저장
        with open(OUTPUT_JSON, 'w', encoding='utf-8') as f:
            json.dump(error, f, ensure_ascii=False, indent=2)
        
        _log(f"✗ 실패: {e}")

if __name__ == "__main__":
    main()