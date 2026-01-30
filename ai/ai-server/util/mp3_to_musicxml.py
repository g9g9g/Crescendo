# util/mp3_to_musicxml.py
import os, sys, argparse, subprocess, tempfile
from pathlib import Path
import numpy as np
import soundfile as sf
from imageio_ffmpeg import get_ffmpeg_exe
from piano_transcription_inference import PianoTranscription, sample_rate
import music21 as m21

def ffmpeg_decode_to_float32(path: str, target_sr: int) -> np.ndarray:
    ffmpeg = get_ffmpeg_exe()
    tmp_fd, tmp_path = tempfile.mkstemp(suffix=".wav")
    os.close(tmp_fd)
    try:
        cmd = [ffmpeg, "-y", "-i", str(path), "-ac", "1", "-ar", str(target_sr),
               "-f", "wav", "-acodec", "pcm_f32le", tmp_path]
        proc = subprocess.run(cmd, capture_output=True, text=True)
        if proc.returncode != 0:
            raise RuntimeError(f"FFmpeg 변환 실패\nstdout:\n{proc.stdout}\nstderr:\n{proc.stderr}")
        audio, sr = sf.read(tmp_path, dtype="float32", always_2d=False)
        if sr != target_sr:
            raise RuntimeError(f"FFmpeg resample mismatch: got {sr}, expect {target_sr}")
        return audio
    finally:
        try: os.remove(tmp_path)
        except OSError: pass

def transcribe_mp3_to_midi(mp3_path: str, midi_out: str, device: str = "cpu"):
    audio = ffmpeg_decode_to_float32(mp3_path, target_sr=sample_rate)
    model = PianoTranscription(device=device)
    model.transcribe(audio, midi_out)

def midi_to_musicxml(midi_path: str, xml_out: str,
                     quantization: str = "16th",
                     infer_key_time: bool = True,
                     simplify_notation: bool = True):
    s = m21.converter.parse(midi_path)
    if infer_key_time:
        try:
            k = s.analyze('key')
            for p in s.parts:
                p.insert(0, m21.key.KeySignature(k.sharps))
        except Exception:
            pass
    if quantization:
        qmap = {"32nd": 0.125, "16th": 0.25, "8th": 0.5, "quarter": 1.0}
        q = qmap.get(quantization, 0.25)
        try:
            s.quantize(quarterLengthDivisors=[q], processGroups=True, inPlace=True)
        except Exception:
            pass
    try: s.makeMeasures(inPlace=True)
    except Exception: pass
    if simplify_notation:
        try: s.makeNotation(inPlace=True)
        except Exception: pass
    s.write('musicxml', fp=xml_out)
